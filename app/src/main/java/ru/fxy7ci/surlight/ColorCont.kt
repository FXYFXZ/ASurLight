package ru.fxy7ci.surlight
import android.graphics.Color

/** Контейнер для работы с лампами
 * =============================== */
enum class HSV_Bytes(val b: Int) {
    BYTE_HUE(0),
    BYTE_SUTUR(1),
    BYTE_VALUE(2)
}

class ColorCont(myRed: Int, myGreen: Int, myBlue: Int) {
    private var curValue = MAX_VAL

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
        hsv[HSV_Bytes.BYTE_VALUE.ordinal] = 0f
    }


    // Двигаем цвет
    fun moveHue(myVal: Float){
        hsv[2] = curValue
        hsv[0] += myVal
        if (hsv[0] < 0) hsv[0] = 0f
        if (hsv[0] > 360) hsv[0] = MAX_HUE
    }

    fun moveValue(myVal: Float){
        if (kotlin.math.abs(myVal) > 1f) return
        hsv[2] = curValue
        hsv[1] += myVal
        if (hsv[2] < 0) hsv[2] = 0f
        if (hsv[2] > 1) hsv[2] = MAX_VAL
    }

    fun settValue (myVal: Float){
        curValue = myVal
    }

    companion object {
        const val MAX_HUE = 360f
        const val MAX_VAL = 1f
        const val  SATURATION = 0.25f
    }

}
