package net.app.mvvmsampleapp.floor.design

import android.animation.ObjectAnimator
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.View.OnTouchListener
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProviders
import io.realm.Realm
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.activity_floor_design.*
import net.app.mvvmsampleapp.R
import net.app.mvvmsampleapp.app.AppData
import net.app.mvvmsampleapp.databinding.ActivityFloorDesignBinding
import net.app.mvvmsampleapp.floor.design.add_table.AddTableListener
import net.app.mvvmsampleapp.floor.design.add_table.DialogAddTable
import net.app.mvvmsampleapp.floor.design.add_table.Table
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance


class DesignActivity : AppCompatActivity(), KodeinAware, AddTableListener,
    RotationGestureDetector.OnRotationGestureListener {

    override val kodein by kodein()
    private val factory: DesignViewModelFactory by instance()
    var realm: Realm? = null

    private enum class GestureMode { NONE, DRAG, ZOOM, ROTATION }

    private var gestureMode = GestureMode.NONE
    var gestureDetector: GestureDetector? = null
    var scaleGestureDetector: ScaleGestureDetector? = null
    private var mRotationDetector: RotationGestureDetector? = null
    private var scaleFactor = 0f
    private val maxScaleAmount = 500
    private val minScaleAmount = 70
    private var rotationAngle = 0f
    private var endAngle = 0f;


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding: ActivityFloorDesignBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_floor_design)
        val viewModel = ViewModelProviders.of(this, factory).get(DesignViewModel::class.java)

        binding.designViewModel = viewModel

        iniObject()

        initSavedTables()
    }


    private fun iniObject() {
        realm = Realm.getDefaultInstance()
        gestureDetector = GestureDetector(this, GestureListener(this, this))
        scaleGestureDetector = ScaleGestureDetector(this, simpleOnScaleGestureListener)
        mRotationDetector = RotationGestureDetector(this);
    }


    private fun initSavedTables() {
        try {
            val results = realm?.where<Table>()?.findAll()
            for (i in 0 until results!!.size) {
                inflateTable(results.get(i)!!.id)
            }
        } catch (error: Exception) {
            Log.d("1111", "Error : ${error.printStackTrace()}")
        }
    }


    fun onAddTableClick(view: View) {
        DialogAddTable(this, this, "").show()
    }

    override fun onAddButtonClickedInDialoge() {
        CL_floor.removeAllViews()
        initSavedTables()
    }

    private fun inflateTable(id: String) {
        val tv_table_name: TextView?

        val layoutInflater: LayoutInflater = LayoutInflater.from(this)
        val layout_tables: View = layoutInflater.inflate(R.layout.layout_tables, CL_floor, false)
        tv_table_name = layout_tables.findViewById(R.id.tv_table_name)

        val table: Table? = TableRepository().getTableById(id)

        layout_tables.x = table!!.position_x
        layout_tables.y = table.position_y

        layout_tables.layoutParams = LinearLayout.LayoutParams(
            table.width,
            table.height
        )

        layout_tables.rotation = table.rotation

        when (table.shape) {
            "Square" -> {
                layout_tables.setBackgroundResource(R.drawable.square)
                tv_table_name.text = "Table  ${table.number}"
            }
            "Circle" -> {
                layout_tables.setBackgroundResource(R.drawable.circle)
                tv_table_name.text = "Table  ${table.number}"
            }

            "Rectangle vertical" -> {
                layout_tables.setBackgroundResource(R.drawable.ractangle)

                layout_tables.layoutParams = LinearLayout.LayoutParams(
                    table.width,
                    table.height
                )

                tv_table_name.text = table.number
            }

            "Rectangle horizontal" -> {
                layout_tables.setBackgroundResource(R.drawable.ractangle)
                layout_tables.layoutParams = LinearLayout.LayoutParams(
                    table.width,
                    table.height
                )

                tv_table_name.text = "Table  ${table.number}"
            }
        }

        layout_tables.setOnTouchListener(initTouchListener(id))
        CL_floor?.addView(layout_tables)
    }

    private fun initTouchListener(id: String): OnTouchListener {
        var dX = 0f
        var dY = 0f
        var _x = 0f
        var _y = 0f

        return OnTouchListener(function = { view, event ->
            //Log.d("1111", "gesture Mode : $gestureMode")

            gestureDetector?.onTouchEvent(event)
            scaleGestureDetector?.onTouchEvent(event)
            mRotationDetector?.onTouchEvent(event)

            when (event.action) {

                MotionEvent.ACTION_DOWN -> {
                    AppData.lastClickedTableId = id
                    gestureMode = GestureMode.DRAG

                    dX = view.x - event.rawX
                    dY = view.y - event.rawY
                }

                MotionEvent.ACTION_MOVE -> {
                    if (gestureMode == GestureMode.DRAG) {
                        _x = event.rawX + dX
                        _y = event.rawY + dY
                        moveTheView(view, _x, _y)

                    } else if (gestureMode == GestureMode.ZOOM) {
                        scaleTheView(view)

                    } else if (gestureMode == GestureMode.ROTATION) {
                        view.rotation += rotationAngle
                    }
                }

                MotionEvent.ACTION_UP -> {
                    if (gestureMode == GestureMode.ROTATION)
                        fixViewRotation(view)

                    updateTable(view)
                    gestureMode = GestureMode.NONE
                    rotationAngle = 0f
                }
            }
            CL_floor.invalidate()

            true
        })
    }


    private fun moveTheView(view: View?, _x: Float, _y: Float) {
        view!!.animate()
            .x(_x)
            .y(_y)
            .setDuration(0)
            .start()

        //updateTable(view)
        view.invalidate()
    }

    private fun scaleTheView(view: View) {
        val scaledWidth: Float
        val scaledHeight: Float
        if (scaleFactor > 1 && view.measuredWidth < maxScaleAmount) {              //zoom in and not reaching max scale
            scaledWidth = view.measuredWidth + scaleFactor * 4
            scaledHeight = view.measuredHeight + scaleFactor * 4
        } else if (scaleFactor < 1 && view.measuredWidth > minScaleAmount) {       //zoom out and not reaching min scale
            scaledWidth = view.measuredWidth - scaleFactor * 4
            scaledHeight = view.measuredHeight - scaleFactor * 4
        } else {
            return
        }

        view.layoutParams = ConstraintLayout.LayoutParams(
            scaledWidth.toInt(),
            scaledHeight.toInt()
        )

        //updateTable(view)

        view.invalidate()
    }

    private fun fixViewRotation(view: View?) {
        val viewRotation = view!!.rotation

        //clock wise rotation
        if (viewRotation > 0f && viewRotation < 45f) {
            endAngle = 0f
        } else if (viewRotation > 45f && viewRotation < 135f) {
            endAngle = 90f
        } else if (viewRotation > 135f && viewRotation < 225f) {
            endAngle = 180f
        } else if (viewRotation > 225f && viewRotation < 315f) {
            endAngle = 270f
        } else if (viewRotation > 315f && viewRotation < 360f) {
            endAngle = 0f

            //anti-clock wise rotation
        } else if (viewRotation < 0f && viewRotation > -45f) {
            endAngle = 0f
        } else if (viewRotation < -45f && viewRotation > -135f) {
            endAngle = -90f
        } else if (viewRotation < -135f && viewRotation > -225f) {
            endAngle = -180f
        } else if (viewRotation < -225f && viewRotation > -315f) {
            endAngle = -270f
        } else if (viewRotation < -315f && viewRotation > -360f) {
            endAngle = 0f
        }

        rotateTheView(view, viewRotation, endAngle)
    }

    private fun rotateTheView(view: View?, startAngle: Float, endAngle: Float) {
        try {
            if (startAngle != endAngle) {
                val rotate = ObjectAnimator.ofFloat(view, "rotation", startAngle, endAngle)
                //rotate.setRepeatCount(10);
                rotate.duration = 400
                rotate.start()
            }
        } catch (error: Exception) {
            Log.d("1111", "Error : ${error.printStackTrace()}")
        } finally {
            view!!.rotation = endAngle
            //updateTable(view)
            view.invalidate()
        }
    }


    private fun updateTable(view: View) {
        TableRepository().updateTableById(
            AppData.lastClickedTableId,
            "",
            "",
            view.measuredWidth,
            view.measuredHeight,
            view.x,
            view.y,
            view.rotation
        )
    }


    private class GestureListener(
        private val context: Context,
        private val add_table_listener: AddTableListener
    ) : GestureDetector.SimpleOnGestureListener() {

        override fun onDown(e: MotionEvent): Boolean {
            return true
        }

        // event when double tap occurs
        override fun onDoubleTap(e: MotionEvent): Boolean {
            DialogAddTable(context, add_table_listener, AppData.lastClickedTableId).show()
            return true
        }
    }


    private val simpleOnScaleGestureListener =
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                scaleFactor = detector.scaleFactor
                //Log.d("1111", "scaleFactor : $scaleFactor")
                return true
            }

            override fun onScaleBegin(detector: ScaleGestureDetector?): Boolean {
                //return super.onScaleBegin(detector)
                gestureMode = GestureMode.ZOOM
                return true;
            }
        }

    override fun OnRotation(rotationDetector: RotationGestureDetector?) {
        rotationAngle = -rotationDetector!!.angle
        //Log.d("1111", "angle : $rotationAngle")

        if (rotationAngle < -5 || rotationAngle > 5) {
            gestureMode = GestureMode.ROTATION
        }
    }


    override fun onDestroy() {
        super.onDestroy()

        realm?.close()
    }


}
