package ru.fxy7ci.surlight
import android.graphics.Color

/** Контейнер для работы с лампами
 * =============================== */

class ColorCont(myRed: Int = 200, myGreen: Int = 200, myBlue: Int =200) {
    private var curValue = MAX_VAL
    private var hsv = FloatArray(3)
    var isDirty = false
    init {
        if (myRed in 0..255 && myGreen in 0..255 && myBlue in 0..255) {
            Color.colorToHSV(Color.rgb(myRed,myGreen,myBlue), hsv)
        }
    }

    // Ставим значение по умолчанию
    fun setDefault(){
        Color.colorToHSV(Color.rgb(200,200,200), hsv)
        isDirty = true
    }

    fun getColor(): Int{
        return Color.HSVToColor(hsv)
    }

    fun setOff(){
        hsv[2] = 0f
        isDirty = true
    }

    // Двигаем цвет
    fun moveHue(myVal: Float){
        hsv[2] = curValue
        
        var cv = hsv[0]
        // 0..360
        cv += myVal
        if (cv < 0) cv = 0f
        if (cv > MAX_HUE) cv = MAX_HUE
        hsv[0] = cv
        isDirty = true
    }

    fun moveSaturation(myVal: Float){
        if (kotlin.math.abs(myVal) > MAX_VAL) return
        hsv[2] = curValue

        var cv = hsv[1]

        cv += myVal
        if (cv < 0) cv = 0f
        if (cv > MAX_VAL) cv = MAX_SAT

        hsv[1] = cv
        isDirty = true
    }

    fun settValue (myVal: Float){
        if (myVal > MAX_VAL) return
        curValue = myVal
        hsv[2] = curValue
        isDirty = true
    }

    companion object {
        const val MAX_HUE = 360f
        const val MAX_VAL = 1f
        const val MAX_SAT = 1f
    }
}
