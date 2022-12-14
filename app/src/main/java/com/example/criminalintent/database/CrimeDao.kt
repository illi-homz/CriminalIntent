package com.example.criminalintent.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.criminalintent.Crime
import java.util.UUID

@Dao
interface CrimeDao {
    @Query("SELECT * FROM crime")
//    fun getCrimes(): List<Crime>
    fun getCrimes(): LiveData<List<Crime>>

    @Query("SELECT * from crime WHERE id=(:id)")
//    fun getCrime(id: UUID): Crime?
    fun getCrime(id: UUID): LiveData<Crime?>

    @Update
    fun updateCrime(crime: Crime)

    @Insert
    fun addCrime(crime: Crime)

    @Query("DELETE FROM crime where id=(:id)")
    fun removeCrime(id: UUID)
}