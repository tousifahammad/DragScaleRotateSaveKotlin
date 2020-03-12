package net.app.mvvmsampleapp.floor.design

import android.util.Log
import io.realm.Realm
import io.realm.kotlin.where
import net.app.mvvmsampleapp.floor.design.add_table.Table
import java.util.*


class TableRepository() {
    val realm: Realm = Realm.getDefaultInstance()


    fun getTableById(id: String): Table? {
        var table: Table? = null
        try {
            table = realm.where<Table>().equalTo("id", id)?.findFirst()!!
        } catch (error: Exception) {
            Log.d("1111", "Error : ${error.printStackTrace()}")
        } finally {

        }
        return table
    }

    fun addTableToDB(
        number: String,
        shape: String,
        width: Int,
        height: Int,
        position_x: Float,
        position_y: Float,
        rotation: Float
    ) {
        try {
            realm.beginTransaction()

            val table: Table = realm.createObject(Table::class.java, UUID.randomUUID().toString())
            table.number = number
            table.shape = shape
            table.width = width
            table.height = height
            table.position_x = position_x
            table.position_y = position_y
            table.rotation = rotation

            realm.commitTransaction()
        } catch (error: Exception) {
            Log.d("1111", "Error : ${error.printStackTrace()}")
        } finally {
            realm.close()
        }

    }

    fun updateTableById(
        id: String,
        number: String,
        shape: String,
        width: Int,
        height: Int,
        position_x: Float,
        position_y: Float,
        rotation: Float
    ) {
        //val realm = Realm.getDefaultInstance()
        try {
            realm.executeTransaction { realm ->
                val table: Table = realm.where<Table>().equalTo("id", id).findFirst()!!
                if (number.isNotEmpty())
                    table.number = number
                if (shape.isNotEmpty())
                    table.shape = shape
                if (width > 0)
                    table.width = width
                if (height > 0)
                    table.height = height
                table.position_x = position_x
                table.position_y = position_y
                table.rotation = rotation
            }
        } catch (error: Exception) {
            Log.d("1111", "Error : ${error.printStackTrace()}")
        } finally {
            realm.close()
        }
    }


    fun clearAllTable() {

    }
}
