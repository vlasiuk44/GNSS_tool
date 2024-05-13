package com.example.ggg2

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.GnssClock
import android.location.GnssMeasurement
import android.location.GnssMeasurementsEvent
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.SyncStateContract.Constants
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlin.math.floor
var NUMBER_NANO_SECONDS_PER_WEEK = 604800000000000L;
var SPEED_OF_LIGHT = 299792458.0
var NumberNanoSeconds100Milli = 1e8
private fun GPSpseudo(clock: GnssClock, measurement: GnssMeasurement): Double {
    val gpsTime = clock.timeNanos - (clock.fullBiasNanos+clock.biasNanos)
    val tRxGPS  = gpsTime + measurement.timeOffsetNanos
    val weekNumberNanos =
        floor(-1.0 * clock.fullBiasNanos / NUMBER_NANO_SECONDS_PER_WEEK) * NUMBER_NANO_SECONDS_PER_WEEK
    val pseudorange = (tRxGPS - weekNumberNanos - measurement.receivedSvTimeNanos) / 1.0E9 * SPEED_OF_LIGHT;
    val measState = measurement.state

    val codeLock = (measState and GnssMeasurement.STATE_CODE_LOCK) > 0
    val towDecoded = (measState and GnssMeasurement.STATE_TOW_DECODED) > 0;

    var MAXTOWUNCNS = 50
    val towUncertainty = measurement.receivedSvTimeUncertaintyNanos < MAXTOWUNCNS;



    return pseudorange

    
}
private fun GALILEOpseudo(clock: GnssClock, measurement: GnssMeasurement): Double {
    val galileoTime = clock.timeNanos - (clock.fullBiasNanos + clock.biasNanos);
    val tRxGalileoTOW = galileoTime % NUMBER_NANO_SECONDS_PER_WEEK;
    val tRxGalileoE1_2nd = galileoTime % NumberNanoSeconds100Milli;
    val tTxGalileo = measurement.receivedSvTimeNanos + measurement.timeOffsetNanos;
    val pseudorangeTOW = (tRxGalileoTOW - tTxGalileo) * 1e-9 * SPEED_OF_LIGHT;
    val pseudorangeE1_2nd = ((galileoTime - tTxGalileo) % NumberNanoSeconds100Milli) * 1e-9 * SPEED_OF_LIGHT;
    val measState = measurement.state
    val towKnown = measState and GnssMeasurement.STATE_TOW_KNOWN > 0
    val towDecoded = measState and GnssMeasurement.STATE_TOW_DECODED > 0
    val codeLock = measState and GnssMeasurement.STATE_GAL_E1C_2ND_CODE_LOCK > 0
    if ((towKnown || towDecoded)) {
        return pseudorangeTOW
        // use pseudorangeTOW

    }else if (codeLock){
        return pseudorangeE1_2nd
        // use pseudorangeE1_2nd

    }


    return 0.0


}

class MainActivity : AppCompatActivity() {

    private lateinit var locationManager: LocationManager
    private var gnssCallback: GnssMeasurementsEvent.Callback? = null
    private lateinit var textView: TextView

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textView = findViewById(R.id.textView)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            gnssCallback = object : GnssMeasurementsEvent.Callback() {
                @RequiresApi(Build.VERSION_CODES.R)
                override fun onGnssMeasurementsReceived(event: GnssMeasurementsEvent) {
                    val measurements: MutableCollection<GnssMeasurement> = event.measurements
                    val stringBuilder = StringBuilder()
                    for (measurement in measurements) {

                        val satelliteId = getSatelliteId(measurement)
                        stringBuilder.append("satelliteId ")
                        stringBuilder.append(satelliteId)
                        stringBuilder.append("\n")

                        stringBuilder.append("state ")
                        stringBuilder.append(measurement.accumulatedDeltaRangeState)
                        stringBuilder.append("\n")

                        stringBuilder.append("CarrierCycles ")
                        stringBuilder.append(measurement.getCarrierCycles())
                        stringBuilder.append("\n")

                        stringBuilder.append("AccumulatedDeltaRangeMeters ")
                        stringBuilder.append(measurement.getAccumulatedDeltaRangeMeters())
                        stringBuilder.append("\n")

                        stringBuilder.append("CarrierFrequencyHz ")
                        stringBuilder.append(measurement.getCarrierFrequencyHz())
                        stringBuilder.append("\n")

                        stringBuilder.append("CarrierPhase ")
                        stringBuilder.append(measurement.carrierPhase)
                        stringBuilder.append("\n\n")

                        //stringBuilder.append("svid $measurement \n")
                        //stringBuilder.append("CarrierCycles $measurement.getCarrierCycles() \n")
                        //stringBuilder.append("CarrierFrequencyHz $measurement.getCarrierFrequencyHz() \n")
                        //stringBuilder.append("CarrierPhase $measurement.getCarrierPhase() \n\n")

//                            if('G' in satelliteId){stringBuilder.append("${GPSpseudo(gnssClock,measurement)} \n\n")}
//                            else if('J' in satelliteId){stringBuilder.append("${GALILEOpseudo(gnssClock,measurement)} \n\n")}


//accumulated delta range = -k * carrier phase

                            //.append("${measurement.pse}\n\n")
                    }
                    val data = stringBuilder.toString()
                    runOnUiThread { textView.text = data }
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            gnssCallback?.let { locationManager.registerGnssMeasurementsCallback(it) }
        } else {
            Toast.makeText(this, "Ваше устройство не поддерживает получение сырых данных GNSS", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (gnssCallback != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                locationManager.unregisterGnssMeasurementsCallback(gnssCallback!!)
            }
        }
    }
}

private fun getSatelliteId(measurement: GnssMeasurement): String {
    val constellation = when (measurement.constellationType) {
        1 -> "G" // CONSTELLATION_GPS
        3 -> "R" // CONSTELLATION_GLONASS
        5 -> "B" // CONSTELLATION_BEIDOU
        6 -> "J" // CONSTELLATION_GALILEO
        else -> "Unknown"
    }
    return "${constellation}${measurement.svid}"
}