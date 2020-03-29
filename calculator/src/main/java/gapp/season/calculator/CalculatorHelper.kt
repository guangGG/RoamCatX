package gapp.season.calculator

import android.app.Activity
import android.content.Context
import android.content.Intent
import bsh.Interpreter

object CalculatorHelper {
    fun openCalculator(context: Context) {
        val intent = Intent(context, CalculatorActivity::class.java)
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun eval(exp: String): Number? {
        try {
            val bsh = Interpreter()
            return bsh.eval(exp) as Number
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun getCalcBuffer(context: Context): String {
        return context.getSharedPreferences("Calculator", Context.MODE_PRIVATE)
                .getString("CalcBuffer", "0") ?: "0"
    }

    fun saveCalcBuffer(context: Context, value: String) {
        context.getSharedPreferences("Calculator", Context.MODE_PRIVATE).edit().putString("CalcBuffer", value).apply()
    }
}
