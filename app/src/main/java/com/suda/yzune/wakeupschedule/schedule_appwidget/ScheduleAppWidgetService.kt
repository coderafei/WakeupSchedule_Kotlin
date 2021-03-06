package com.suda.yzune.wakeupschedule.schedule_appwidget

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import com.suda.yzune.wakeupschedule.AppDatabase
import com.suda.yzune.wakeupschedule.R
import com.suda.yzune.wakeupschedule.bean.CourseBean
import com.suda.yzune.wakeupschedule.bean.TableBean
import com.suda.yzune.wakeupschedule.bean.TimeDetailBean
import com.suda.yzune.wakeupschedule.utils.CourseUtils.countWeek
import com.suda.yzune.wakeupschedule.utils.ViewUtils
import com.suda.yzune.wakeupschedule.widget.TipTextView
import org.jetbrains.anko.dip
import java.text.ParseException

class ScheduleAppWidgetService : RemoteViewsService() {
    private lateinit var table: TableBean

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return ScheduleRemoteViewsFactory()
    }

    private inner class ScheduleRemoteViewsFactory : RemoteViewsService.RemoteViewsFactory {
        private var week = 0
        private var widgetItemHeight = 0
        private var marTop = 0
        private var alphaStr = ""
        private val dataBase = AppDatabase.getDatabase(applicationContext)
        private val tableDao = dataBase.tableDao()
        private val baseDao = dataBase.courseBaseDao()
        private val timeDao = dataBase.timeDetailDao()
        private val timeList = arrayListOf<TimeDetailBean>()

        override fun onCreate() {
            table = tableDao.getDefaultTableInThread()
            widgetItemHeight = dip(table.widgetItemHeight.toFloat())
            marTop = resources.getDimensionPixelSize(R.dimen.weekItemMarTop)
            val alphaInt = Math.round(255 * (table.widgetItemAlpha.toFloat() / 100))
            alphaStr = if (alphaInt != 0) {
                Integer.toHexString(alphaInt)
            } else {
                "00"
            }
            if (alphaStr.length < 2) {
                alphaStr = "0$alphaStr"
            }
            if (table.showTime) {
                timeList.clear()
                timeList.addAll(timeDao.getTimeListInThread(table.timeTable))
            } else {
                timeList.clear()
            }
        }

        override fun onDataSetChanged() {
            table = tableDao.getDefaultTableInThread()
            widgetItemHeight = dip(table.widgetItemHeight)
            marTop = resources.getDimensionPixelSize(R.dimen.weekItemMarTop)
            val alphaInt = Math.round(255 * (table.widgetItemAlpha.toFloat() / 100))
            alphaStr = if (alphaInt != 0) {
                Integer.toHexString(alphaInt)
            } else {
                "00"
            }
            if (alphaStr.length < 2) {
                alphaStr = "0$alphaStr"
            }
            if (table.showTime) {
                timeList.clear()
                timeList.addAll(timeDao.getTimeListInThread(table.timeTable))
            } else {
                timeList.clear()
            }
        }

        override fun onDestroy() {

        }

        override fun getCount(): Int {
            return 1
        }

        override fun getViewAt(position: Int): RemoteViews {
            val mRemoteViews = RemoteViews(applicationContext.packageName, R.layout.item_schedule_widget)
            initData(applicationContext, mRemoteViews)
            return mRemoteViews
        }

        override fun getLoadingView(): RemoteViews? {
            return null
        }

        override fun getViewTypeCount(): Int {
            return 1
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun hasStableIds(): Boolean {
            return false
        }

        fun initView(view: View, weekPanel0: View) {
            val weekPanel7 = view.findViewById<View>(R.id.anko_ll_week_panel_7)
            val weekPanel6 = view.findViewById<View>(R.id.anko_ll_week_panel_6)

            if (table.showSun) {
                if (table.sundayFirst) {
                    weekPanel7.visibility = View.GONE
                    weekPanel0.visibility = View.VISIBLE
                } else {
                    weekPanel7.visibility = View.VISIBLE
                    weekPanel0.visibility = View.GONE
                }
            } else {
                weekPanel7.visibility = View.GONE
                weekPanel0.visibility = View.GONE
            }

            if (table.showSat) {
                weekPanel6.visibility = View.VISIBLE
            } else {
                weekPanel6.visibility = View.GONE
            }

            for (i in 0 until 20) {
                val tv = view.findViewById<TextView>(R.id.anko_tv_node1 + i)
                val lp = tv.layoutParams
                lp.height = widgetItemHeight
                tv.layoutParams = lp
                tv.setTextColor(table.widgetTextColor)
                if (i >= table.nodes) {
                    tv.visibility = View.GONE
                } else {
                    tv.visibility = View.VISIBLE
                }
            }
        }

        fun initData(context: Context, views: RemoteViews) {
            try {
                week = countWeek(table.startDate, table.sundayFirst)
            } catch (e: ParseException) {
                e.printStackTrace()
            }

            if (week <= 0) {
                week = 1
            }

            val view = ViewUtils.createScheduleView(context)
            val weekPanel0 = view.findViewById<LinearLayout>(R.id.anko_ll_week_panel_0)
            initView(view, weekPanel0)

            for (i in 1..7) {
                val list = baseDao.getCourseByDayOfTableInThread(i, table.id)
                initWeekPanel(weekPanel0, context, view, list, i)
            }
            val scrollView = view.findViewById<ScrollView>(R.id.anko_sv_schedule)
            ViewUtils.layoutView(scrollView, dip(375), dip(375))
            views.setBitmap(R.id.iv_schedule, "setImageBitmap", ViewUtils.getViewBitmap(scrollView))
        }

        private fun initWeekPanel(weekPanel0: LinearLayout, context: Context, view: View, data: List<CourseBean>?, day: Int) {
            val llIndex = day - 1
            val ll = view.findViewById<LinearLayout>(R.id.anko_ll_week_panel_1 + llIndex)
            ll.removeAllViews()
            if (data == null || data.isEmpty()) return
            var isCovered = false
            var pre = data[0]
            for (i in data.indices) {
                val strBuilder = StringBuilder()
                val c = data[i]
                val tv = TipTextView(context)
                val lp = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        widgetItemHeight * c.step + marTop * (c.step - 1))
                if (i > 0) {
                    lp.setMargins(0, (c.startNode - (pre.startNode + pre.step)) * (widgetItemHeight + marTop) + marTop, 0, 0)
                } else {
                    lp.setMargins(0, (c.startNode - 1) * (widgetItemHeight + marTop) + marTop, 0, 0)
                    isCovered = (pre.startNode == c.startNode)
                }
                tv.layoutParams = lp
                //tv.gravity = Gravity.CENTER_VERTICAL
                tv.textSize = table.widgetItemTextSize.toFloat()
                tv.typeface = Typeface.defaultFromStyle(Typeface.BOLD)
                tv.setPadding(8, 8, 8, 8)
                tv.setTextColor(table.widgetCourseTextColor)

                tv.background = ContextCompat.getDrawable(context.applicationContext, R.drawable.course_item_bg)
                val myGrad = tv.background as GradientDrawable
                myGrad.setStroke(dip(2), table.widgetStrokeColor)

                when {
                    c.color.length == 7 -> myGrad.setColor(Color.parseColor("#$alphaStr${c.color.substring(1, 7)}"))
                    c.color.isEmpty() -> {
                        myGrad.setColor(Color.parseColor("#${alphaStr}fa6278"))
                    }
                    else -> myGrad.setColor(Color.parseColor("#$alphaStr${c.color.substring(3, 9)}"))
                }

                strBuilder.append(c.courseName)

                if (c.room != "") {
                    strBuilder.append("@${c.room}")
                }

                when (c.type) {
                    1 -> {
                        strBuilder.append("\n单周")
                        if (week % 2 == 0) {
                            if (table.showOtherWeekCourse) {
                                strBuilder.append("\n单周[非本周]")
                                tv.visibility = View.VISIBLE
                                tv.alpha = 0.6f
                                myGrad.setColor(ContextCompat.getColor(context.applicationContext, R.color.grey))
                            } else {
                                tv.visibility = View.INVISIBLE
                            }
                        }
                    }
                    2 -> {
                        strBuilder.append("\n双周")
                        if (week % 2 != 0) {
                            if (table.showOtherWeekCourse) {
                                tv.alpha = 0.6f
                                strBuilder.append("\n双周[非本周]")
                                tv.visibility = View.VISIBLE
                                myGrad.setColor(ContextCompat.getColor(context.applicationContext, R.color.grey))
                            } else {
                                tv.visibility = View.INVISIBLE
                            }
                        }
                    }
                }

                if (c.startWeek > week || c.endWeek < week) {
                    if (table.showOtherWeekCourse) {
                        tv.alpha = 0.6f
                        if (!strBuilder.endsWith("[非本周]")) {
                            strBuilder.append("[非本周]")
                        }
                        tv.visibility = View.VISIBLE
                        myGrad.setColor(ContextCompat.getColor(context.applicationContext, R.color.grey))
                    } else {
                        tv.visibility = View.INVISIBLE
                    }
                }

                if (!strBuilder.endsWith("[非本周]") && ll.findViewWithTag<TextView?>("第${c.startNode}节") == null) {
                    tv.tag = "第${c.startNode}节"
                }

                if (isCovered) {
                    if (ll.getChildAt(ll.childCount - 1) != null) {
                        if (ll.getChildAt(ll.childCount - 1).alpha < 0.8f) {
                            ll.getChildAt(ll.childCount - 1).visibility = View.INVISIBLE
                        }
                    }
                }

                if (ll.findViewWithTag<TextView?>("第${c.startNode}节") != null) {
                    tv.visibility = View.INVISIBLE
                    val textView = ll.findViewWithTag<TipTextView>("第${c.startNode}节")
                    if (textView.tipVisibility == TipTextView.TIP_INVISIBLE) {
                        textView.tipVisibility = TipTextView.TIP_VISIBLE
                    }
                }

                if (timeList.isNotEmpty()) {
                    strBuilder.insert(0, timeList[c.startNode - 1].startTime + "\n")
                }

                tv.text = strBuilder
                if (day == 7) {
                    if (table.sundayFirst) {
                        weekPanel0.addView(tv)
                    } else {
                        ll.addView(tv)
                    }
                } else {
                    ll.addView(tv)
                }
                pre = c
            }
        }

    }

}