package kraemer.leonard.de.smoothedz_score

import android.graphics.Color.parseColor
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.android.synthetic.main.activity_smoothed_zscore.*
import org.apache.commons.math3.stat.descriptive.SummaryStatistics
import java.util.*

class SmoothedZScore : AppCompatActivity() {
    // Data
    //@formatter:off
    val y = listOf(1.0, 1.0, 1.1, 1.0, 0.9, 1.0, 1.0, 1.1, 1.0, 0.9, 1.0, 1.1, 1.0, 1.0, 0.9, 1.0,
                   1.0, 1.1,1.0, 1.0,1.0, 1.0, 1.1, 0.9, 1.0, 1.1, 1.0, 1.0, 0.9, 1.0, 1.1, 1.0, 1.0, 1.1, 1.0, 0.8,
                   0.9, 1.0, 1.2, 0.9, 1.0,1.0, 1.1, 1.2, 1.0, 1.5, 1.0, 3.0, 2.0, 5.0, 3.0, 2.0, 1.0, 1.0, 1.0,
                   0.9, 1.0,1.0, 3.0, 2.6, 4.0, 3.0, 3.2, 2.0, 1.0, 1.0, 0.8, 4.0, 4.0, 2.0, 2.5, 1.0, 1.0, 1.0)
    //@formatter:on

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_smoothed_zscore)

        // Settings
        val lag = 30
        val threshold = 5.0f
        val influence = 0.0f
        val thresholdingResults = smoothedZScore(y, lag, threshold.toDouble(), influence.toDouble())
        val signal = thresholdingResults.first.map { it.toFloat() }
        val avgFilter = thresholdingResults.second.map { it.toFloat() }
        val stdFilter = thresholdingResults.third.map { it.toFloat() }

        val data_Y = LineDataSet(y.mapIndexed { i, it -> Entry(i.toFloat(), it.toFloat()) }, "y")
        val data_avg = LineDataSet(avgFilter.mapIndexed { i, it ->Entry(i.toFloat(), it)}, "avg")
        val data_avg_p = LineDataSet(avgFilter.mapIndexed { i, it ->Entry(i.toFloat(), it + threshold * stdFilter[i])}, "avg + std")
        val data_avg_m = LineDataSet(avgFilter.mapIndexed { i, it ->Entry(i.toFloat(), it - threshold * stdFilter[i])}, "avg - std")
        data_Y.color = parseColor("purple")
        data_avg.color = parseColor("blue")
        data_avg_p.color = parseColor("red")
        data_avg_m.color = parseColor("#FFA500")//orange
        chart1.data = LineData(data_Y, data_avg, data_avg_p, data_avg_m)
        chart1.data.setDrawValues(false)
        chart1.description.isEnabled = false
        chart1.data.dataSets.forEach {(it as LineDataSet).setDrawCircles(false) }
        chart2.data = LineData(LineDataSet(y.mapIndexed { i, it -> Entry(i.toFloat(), signal[i]) }, "signal"))
        chart2.data.setDrawValues(false)
        chart2.description.isEnabled = false
        with(chart2.data.dataSets[0] as LineDataSet) {setDrawCircles(false); color = parseColor("#009917")} //green
    }
}

/**
 * Smoothed zero-score alogrithm shamelessly copied from https://stackoverflow.com/a/22640362/6029703
 * Uses a rolling mean and a rolling deviation (separate) to identify peaks in a vector
 *
 * @param y - The input vector to analyze
 * @param lag - The lag of the moving window (i.e. how big the window is)
 * @param threshold - The z-score at which the algorithm signals (i.e. how many standard deviations away from the moving mean a peak (or signal) is)
 * @param influence - The influence (between 0 and 1) of new signals on the mean and standard deviation (how much a peak (or signal) should affect other values near it)
 * @return - The calculated averages (avgFilter) and deviations (stdFilter), and the signals (signals)
 */
fun smoothedZScore(y: List<Double>, lag: Int, threshold: Double, influence: Double):
    Triple<List<Int>, List<Double>, List<Double>> {
    val stats = SummaryStatistics()

    // the results (peaks, 1 or -1) of our algorithm
    val signals = MutableList<Int>(y.size, { 0 })

    // filter out the signals (peaks) from our original list (using influence arg)
    val filteredY = ArrayList<Double>(y)

    // the current average of the rolling window
    val avgFilter = MutableList<Double>(y.size, { 0.0 })

    // the current standard deviation of the rolling window
    val stdFilter = MutableList<Double>(y.size, { 0.0 })

    // init avgFilter and stdFilter
    y.take(lag).forEach { s -> stats.addValue(s) }

    avgFilter[lag - 1] = stats.mean
    stdFilter[lag - 1] = Math.sqrt(stats.populationVariance) // getStandardDeviation() uses sample variance (not what we want)

    stats.clear()
    //loop input starting at end of rolling window
    (lag..y.size - 1).forEach { i ->
        //if the distance between the current value and average is enough standard deviations (threshold) away
        if (Math.abs(y[i] - avgFilter[i - 1]) > threshold * stdFilter[i - 1]) {
            //this is a signal (i.e. peak), determine if it is a positive or negative signal
            signals[i] = if (y[i] > avgFilter[i - 1]) 1 else -1
            //filter this signal out using influence
            filteredY[i] = (influence * y[i]) + ((1 - influence) * filteredY[i - 1])
        } else {
            //ensure this signal remains a zero
            signals[i] = 0
            //ensure this value is not filtered
            filteredY[i] = y[i]
        }
        //update rolling average and deviation
        (i - lag..i - 1).forEach { stats.addValue(filteredY[it]) }
        avgFilter[i] = stats.getMean()
        stdFilter[i] = Math.sqrt(stats.getPopulationVariance()) //getStandardDeviation() uses sample variance (not what we want)
        stats.clear()
    }
    return Triple(signals, avgFilter, stdFilter)
}
