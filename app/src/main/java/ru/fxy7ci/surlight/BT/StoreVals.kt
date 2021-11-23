package ru.fxy7ci.surlight.BT

//TODO работа по сохранению настроек

class StoreVals {

    companion object {
        const val APP_PREFERENCES = "myAppSettings"
        const val DeviceAddress = "88:25:83:F1:1D:07" //MAC
        const val BT_REQUEST_PERMISSION : Int = 89

        // состояния связи
        const val STATE_DISCONNECTED = 0
        const val STATE_CONNECTING = 1
        const val STATE_CONNECTED = 2
        // характеристика для передачи BLE
        const val BT_MAIN_CHR = "0000ffe1-0000-1000-8000-00805f9b34fb"

    }

}