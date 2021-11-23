package ru.fxy7ci.girtago
import android.graphics.Color
import android.widget.Button
import androidx.core.graphics.red

/** Контейнер для работы с лампами
 * =============================== */

class ColorCont(myRed: Int, myGreen: Int, myBlue: Int) {
    private var curHue = SATURATION

    private var hsv = FloatArray(3)
    init {
        setDefault()
    }

    init {
        Color.colorToHSV(Color.rgb(myRed,myGreen,myBlue), hsv)
    }

    // Ставим значение по умолчанию
    fun setDefault(){
        Color.colorToHSV(Color.rgb(200,200,200), hsv)
    }

    fun getColor(): Int{
        return Color.HSVToColor(hsv)
    }

    fun setOff(){
        hsv[2] = 0f
    }


    // Двигаем цвет
    fun moveHue(myVal: Float){
        hsv[1] = curHue
        hsv[0] += myVal
        if (hsv[0] < 0) hsv[0] = 0f
        if (hsv[0] > 360) hsv[0] = MAX_HUE
    }

    fun moveValue(myVal: Float){
        if (kotlin.math.abs(myVal) > 1f) return
        hsv[1] = curHue
        hsv[2] += myVal
        if (hsv[2] < 0) hsv[2] = 0f
        if (hsv[2] > 1) hsv[2] = MAX_VAL
    }

    fun settSaturation (myVal: Float){
        curHue = myVal
    }

    companion object {
        const val MAX_HUE = 360f
        const val MAX_VAL = 1f
        const val  SATURATION = 0.25f
    }

}
