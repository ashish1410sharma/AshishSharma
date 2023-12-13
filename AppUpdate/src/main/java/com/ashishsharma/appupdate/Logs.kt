package com.ashishsharma.appupdate

import android.util.Log

class Logs {
    companion object{
        fun log(str:String){
//            if(BuildConfig.DEBUG)
                Log.i("PRINT", str)
        }
    }
}