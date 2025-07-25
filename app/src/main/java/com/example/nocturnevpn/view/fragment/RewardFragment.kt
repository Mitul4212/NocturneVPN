package com.example.nocturnevpn.view.fragment

import android.animation.ObjectAnimator
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nocturnevpn.R
import com.example.nocturnevpn.adapter.CoinHistoryAdapter
import com.example.nocturnevpn.databinding.FragmentRewardBinding
import com.example.nocturnevpn.model.CoinHistory
import com.example.nocturnevpn.model.HistoryType
import com.google.android.gms.common.util.CollectionUtils.listOf
import java.text.SimpleDateFormat
import java.util.*
import android.animation.ValueAnimator
import org.json.JSONArray
import org.json.JSONObject
import android.content.SharedPreferences
import android.widget.ImageView
import androidx.navigation.fragment.findNavController
import android.os.Vibrator
import android.os.VibrationEffect

private val KEY_COIN_BALANCE = "coin_balance"
private val KEY_COIN_HISTORY = "coin_history"
private val KEY_AD_WATCH_COUNT = "ad_watch_count"
private val KEY_AD_WATCH_DATE = "ad_watch_date"
private val KEY_PRO_TIMER_END = "pro_timer_end"
private val KEY_PRO_TIMER_TYPE = "pro_timer_type"

class RewardFragment : Fragment() {

    private var _binding: FragmentRewardBinding?= null
    private val binding get() = _binding!!
    private val PREFS = "reward_prefs"
    private val KEY_STREAK = "checkin_streak"
    private val KEY_LAST_DATE = "last_checkin_date"
    private val STRONG_VIOLET = R.color.strong_violet
    private val DEFAULT_BG = R.drawable.checkin_btn
    private val CHECKED_BG = R.color.strong_violet
    private val DEFAULT_REWARD_COLOR = R.color.gold
    private val DEFAULT_DAY_COLOR = R.color.gray

    private val handler = Handler(Looper.getMainLooper())
    private var isAdRunning = false

    private var historyList = mutableListOf<CoinHistory>()
    private lateinit var adapter: CoinHistoryAdapter
    private var displayedCount = 5 // Start with 5

    private val CHECKIN_PREFS = "checkin_prefs"
    private val KEY_CHECKED_DATES = "checked_dates"
    private val CHECKIN_REWARD = 5

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRewardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        android.util.Log.e("DAILY_CHECKIN_DEBUG", "RewardFragment onResume called")
        setupStreakCheckIn()
        setupWatchAdSection()
    }

    private fun saveCoinBalance(balance: Int) {
        val prefs = requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_COIN_BALANCE, balance).apply()
    }

    private fun loadCoinBalance(): Int {
        val prefs = requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_COIN_BALANCE, 0)
    }

    private fun saveCoinHistory() {
        val prefs = requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val arr = JSONArray()
        historyList.forEach {
            val obj = JSONObject()
            obj.put("type", it.type.name)
            obj.put("amount", it.amount)
            obj.put("date", it.date)
            obj.put("description", it.description)
            arr.put(obj)
        }
        prefs.edit().putString(KEY_COIN_HISTORY, arr.toString()).apply()
    }

    private fun loadCoinHistory(): MutableList<CoinHistory> {
        val prefs = requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val arrStr = prefs.getString(KEY_COIN_HISTORY, null) ?: return mutableListOf()
        val arr = JSONArray(arrStr)
        val list = mutableListOf<CoinHistory>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val type = HistoryType.valueOf(obj.getString("type"))
            val amount = obj.getInt("amount")
            val date = obj.getString("date")
            val desc = obj.getString("description")
            list.add(CoinHistory(type, amount, date, desc))
        }
        return list
    }

    private fun getTodayDate(): String {
        return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
    }

    private fun getCheckedDates(prefs: SharedPreferences): MutableSet<String> {
        return prefs.getStringSet(KEY_CHECKED_DATES, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
    }

    private fun saveCheckedDates(prefs: SharedPreferences, dates: Set<String>) {
        prefs.edit().putStringSet(KEY_CHECKED_DATES, dates).apply()
    }

    private fun getYesterdayDate(): String {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DATE, -1)
        return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(cal.time)
    }

    private fun updateStreakUI(streak: Int, enableCheckin: Boolean) {
        val dayBtns = listOf(binding.day1Btn, binding.day2Btn, binding.day3Btn, binding.day4Btn, binding.day5Btn, binding.day6Btn, binding.day7Btn)
        val checkinDays = listOf(
            binding.checkinDay1, binding.checkinDay2, binding.checkinDay3, binding.checkinDay4,
            binding.checkinDay5, binding.checkinDay6, binding.checkinDay7
        )
        val checkinRewards = listOf(
            binding.checkinReward1, binding.checkinReward2, binding.checkinReward3, binding.checkinReward4,
            binding.checkinReward5, binding.checkinReward6, binding.checkinReward7
        )
        val checkmarks = listOf(
            binding.icCheckDay1, binding.icCheckDay2, binding.icCheckDay3, binding.icCheckDay4,
            binding.icCheckDay5, binding.icCheckDay6, binding.icCheckDay7
        )
        for (i in 0..6) {
            if (i < streak) {
                // Checked in
                checkmarks[i].visibility = View.VISIBLE
                checkinDays[i].setTextColor(resources.getColor(android.R.color.white, null))
                checkinRewards[i].setTextColor(resources.getColor(android.R.color.white, null))
                dayBtns[i].setBackgroundResource(R.drawable.bg_checkedin_card)
            } else {
                // Not checked in
                checkmarks[i].visibility = View.GONE
                checkinDays[i].setTextColor(resources.getColor(DEFAULT_DAY_COLOR, null))
                checkinRewards[i].setTextColor(resources.getColor(DEFAULT_REWARD_COLOR, null))
                dayBtns[i].setBackgroundResource(DEFAULT_BG)
            }
            dayBtns[i].alpha = 1.0f
            // Only current streak day is clickable/enabled if enableCheckin is true
            dayBtns[i].isEnabled = (enableCheckin && i == streak)
            dayBtns[i].isClickable = (enableCheckin && i == streak)
            if (enableCheckin && i == streak) {
                bounceView(dayBtns[i])
            }
        }
    }

    // Returns the logical 'today' for check-in (if before 1 AM, returns yesterday)
    private fun getCheckinLogicalToday(): String {
        val cal = java.util.Calendar.getInstance()
        val now = cal.time
        val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
        if (hour < 1) {
            cal.add(java.util.Calendar.DATE, -1)
        }
        return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(cal.time)
    }

    private fun setupStreakCheckIn() {
        val prefs = requireContext().getSharedPreferences(CHECKIN_PREFS, Context.MODE_PRIVATE)
        val streak = prefs.getInt(KEY_STREAK, 0)
        val lastDate = prefs.getString(KEY_LAST_DATE, "") ?: ""
        val logicalToday = getCheckinLogicalToday()
        val logicalYesterday = getYesterdayDateForCheckin()
        var currentStreak = streak
        var enableCheckin = false
        // If never checked in or missed a day, reset
        if (lastDate.isEmpty() || (lastDate != logicalToday && lastDate != logicalYesterday)) {
            currentStreak = 0
            prefs.edit().putInt(KEY_STREAK, 0).putString(KEY_LAST_DATE, "").apply()
            enableCheckin = true // allow day 1
        } else if (lastDate == logicalYesterday) {
            // Continue streak, allow check-in for next day
            enableCheckin = true
        } else if (lastDate == logicalToday) {
            // Already checked in today, do not allow check-in
            enableCheckin = false
        }
        updateStreakUI(currentStreak, enableCheckin)
        val dayBtns = listOf(binding.day1Btn, binding.day2Btn, binding.day3Btn, binding.day4Btn, binding.day5Btn, binding.day6Btn, binding.day7Btn)
        dayBtns.forEachIndexed { idx, btn ->
            btn.setOnClickListener {
                if (enableCheckin && idx == currentStreak) {
                    val reward = if (idx == 6) {
                        val rand = java.util.Random().nextInt(100)
                        val value = if (rand < 80) {
                            10 + java.util.Random().nextInt(21)
                        } else {
                            31 + java.util.Random().nextInt(20)
                        }
                        val animator = android.animation.ValueAnimator.ofInt(0, value)
                        animator.duration = 1000
                        animator.addUpdateListener { valueAnimator ->
                            binding.checkinReward7.text = valueAnimator.animatedValue.toString()
                        }
                        animator.start()
                        try {
                            val vibrator = requireContext().getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                vibrator.vibrate(android.os.VibrationEffect.createOneShot(100, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                            } else {
                                vibrator.vibrate(100)
                            }
                        } catch (e: Exception) { /* ignore */ }
                        value
                    } else {
                        val rewardText = when(idx) {
                            0 -> binding.checkinReward1.text.toString()
                            1 -> binding.checkinReward2.text.toString()
                            2 -> binding.checkinReward3.text.toString()
                            3 -> binding.checkinReward4.text.toString()
                            4 -> binding.checkinReward5.text.toString()
                            5 -> binding.checkinReward6.text.toString()
                            else -> "5"
                        }
                        rewardText.toIntOrNull() ?: 5
                    }
                    val newStreak = currentStreak + 1
                    val logicalTodayNow = getCheckinLogicalToday()
                    prefs.edit().putInt(KEY_STREAK, newStreak).putString(KEY_LAST_DATE, logicalTodayNow).apply()
                    updateStreakUI(newStreak, false)
                    val coinPrefs = requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    val oldBalance = coinPrefs.getInt(KEY_COIN_BALANCE, 0)
                    val newBalance = oldBalance + reward
                    coinPrefs.edit().putInt(KEY_COIN_BALANCE, newBalance).apply()
                    animateCoinBalance(oldBalance, newBalance)
                    val todayDate = getTodayDate()
                    historyList.add(0, CoinHistory(HistoryType.EARN, reward, todayDate, "Daily Check-in"))
                    updateDisplayedHistory()
                    saveCoinHistory()
                    if (newStreak == 7) {
                        Toast.makeText(requireContext(), "Week complete! You got $reward coins! Start again tomorrow!", Toast.LENGTH_LONG).show()
                        prefs.edit().putInt(KEY_STREAK, 0).putString(KEY_LAST_DATE, "").apply()
                    } else {
                        Toast.makeText(requireContext(), "Check-in successful! +$reward coins", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // Returns the logical 'yesterday' for check-in (if before 1 AM, returns two days ago)
    private fun getYesterdayDateForCheckin(): String {
        val cal = java.util.Calendar.getInstance()
        val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
        if (hour < 1) {
            cal.add(java.util.Calendar.DATE, -2)
        } else {
            cal.add(java.util.Calendar.DATE, -1)
        }
        return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(cal.time)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Load coin balance and history from SharedPreferences
        val savedBalance = loadCoinBalance()
        binding.coinBalance.text = savedBalance.toString()
        historyList = loadCoinHistory()
        adapter = CoinHistoryAdapter(mutableListOf())
        binding.coinHistoryRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.coinHistoryRecyclerView.adapter = adapter
        updateDisplayedHistory()
        updateHistoryVisibility()
        setupWatchAdSection()
        setupUseYourCoinSection()
        setupStreakCheckIn()

        // More button logic
        binding.moreButton.setOnClickListener {
            val nextCount = displayedCount + 10
            if (nextCount <= historyList.size) {
                displayedCount = nextCount
            } else {
                displayedCount = historyList.size
            }
            updateDisplayedHistory()
            // Optionally hide button if all loaded
            if (displayedCount >= historyList.size) {
                binding.moreButton.visibility = View.GONE
            }
        }
        // Show/hide more button initially
        binding.moreButton.visibility = if (historyList.size > displayedCount) View.VISIBLE else View.GONE

        // Force update check-in UI with latest streak
        val prefs = requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val streak = prefs.getInt(KEY_STREAK, 0)
        val checkmarks = listOf(
            binding.icCheckDay1, binding.icCheckDay2, binding.icCheckDay3, binding.icCheckDay4,
            binding.icCheckDay5, binding.icCheckDay6, binding.icCheckDay7
        )
        // updateCheckinUI(checkmarks, streak) // Removed
        // setCheckinButtonsEnabled(dayBtns, streak, canCheckInToday) // Removed
        // bounceView(dayBtns[safeStreak]) // Removed

        // Set week_dates TextView to current week range
        binding.weekDates.text = getCurrentWeekRange()
        // Close button logic
        binding.closebtn.setOnClickListener {
            try {
                findNavController().popBackStack()
            } catch (e: Exception) {
                requireActivity().finish()
            }
        }
    }

    // Helper function to get current week range as string (e.g., 7-7 ~ 7-13)
    private fun getCurrentWeekRange(): String {
        val calendar = Calendar.getInstance()
        // Set to first day of week (Sunday)
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        val start = calendar.time
        // Set to last day of week (Saturday)
        calendar.add(Calendar.DAY_OF_WEEK, 6)
        val end = calendar.time
        val sdf = java.text.SimpleDateFormat("M-d", Locale.getDefault())
        return "${sdf.format(start)} ~ ${sdf.format(end)}"
    }

    private fun setupCoinHistory() {
        // No longer needed, handled in onViewCreated
    }

    private fun updateHistoryVisibility() {
        if (historyList.isEmpty()) {
            binding.noCoinHistoryText.visibility = View.VISIBLE
            binding.coinHistoryRecyclerView.visibility = View.GONE
        } else {
            binding.noCoinHistoryText.visibility = View.GONE
            binding.coinHistoryRecyclerView.visibility = View.VISIBLE
        }
    }

    private fun updateDisplayedHistory() {
        val toShow = historyList.take(displayedCount)
        adapter.updateList(toShow)
        binding.moreButton.visibility = if (historyList.size > displayedCount) View.VISIBLE else View.GONE
    }

    // Remove all old daily check-in logic, including:
    // - setupDailyCheckin()
    // - updateCheckinUI()
    // - setCheckinButtonsEnabled()
    // - bounceView()
    // - any related state/fields (e.g., KEY_STREAK, KEY_LAST_DATE, isAdRunning, handler, etc.)
    // - all usages and calls to these functions in lifecycle methods

    private fun setupWatchAdSection() {
        val prefs = requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        var adWatchCount = prefs.getInt(KEY_AD_WATCH_COUNT, 0)
        var adWatchDate = prefs.getString(KEY_AD_WATCH_DATE, "")
        val adReward = 250
        val maxAdsPerDay = 5

        // Reset count if new day
        if (adWatchDate != today) {
            adWatchCount = 0
            adWatchDate = today
            prefs.edit().putInt(KEY_AD_WATCH_COUNT, 0).putString(KEY_AD_WATCH_DATE, today).apply()
        }

        binding.adWatchCount.text = adWatchCount.toString()
        val progressBar = binding.root.findViewById<com.google.android.material.progressindicator.LinearProgressIndicator>(R.id.watchedAdProgressBar)
        progressBar?.max = maxAdsPerDay
        progressBar?.progress = adWatchCount

        binding.watchAdButton.isEnabled = adWatchCount < maxAdsPerDay

        binding.watchAdButton.setOnClickListener {
            // Check/reset date again in case user leaves app open across midnight
            val now = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            var count = prefs.getInt(KEY_AD_WATCH_COUNT, 0)
            var date = prefs.getString(KEY_AD_WATCH_DATE, "")
            if (date != now) {
                count = 0
                date = now
                prefs.edit().putInt(KEY_AD_WATCH_COUNT, 0).putString(KEY_AD_WATCH_DATE, now).apply()
                binding.adWatchCount.text = "0"
                progressBar?.progress = 0
            }
            if (count >= maxAdsPerDay) return@setOnClickListener
            binding.watchAdButton.isEnabled = false
            Toast.makeText(requireContext(), "Ad running...", Toast.LENGTH_LONG).show()
            // Animate progress bar over 5 seconds
            progressBar?.progress = count
            val animator = ValueAnimator.ofInt(count, count + 1)
            animator.duration = 5000
            animator.addUpdateListener { valueAnimator ->
                progressBar?.progress = valueAnimator.animatedValue as Int
            }
            animator.start()
            handler.postDelayed({
                // Add coins
                val previousBalance = loadCoinBalance()
                val newBalance = previousBalance + adReward
                animateCoinBalance(previousBalance, newBalance)
                saveCoinBalance(newBalance)
                // Add to history
                val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                historyList.add(0, CoinHistory(HistoryType.EARN, adReward, todayDate, "Watched Ad"))
                updateDisplayedHistory()
                saveCoinHistory()
                // Update ad watch count
                val newCount = count + 1
                prefs.edit().putInt(KEY_AD_WATCH_COUNT, newCount).putString(KEY_AD_WATCH_DATE, now).apply()
                binding.adWatchCount.text = newCount.toString()
                progressBar?.progress = newCount
                binding.watchAdButton.isEnabled = newCount < maxAdsPerDay
            }, 5000)
        }
    }

    private fun setupUseYourCoinSection() {
        val prefs = requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        binding.halfHourClimeBTN.setOnClickListener {
            useCoinForProTimer(500, 30 * 60 * 1000L, "30m", prefs, "30 mins Premium Server")
        }
        binding.oneHourClimeBTN.setOnClickListener {
            useCoinForProTimer(1000, 60 * 60 * 1000L, "1h", prefs, "1 hour Premium Server")
        }
        binding.oneDayClimeBTN.setOnClickListener {
            useCoinForProTimer(2000, 24 * 60 * 60 * 1000L, "1d", prefs, "1 day Premium Server")
        }
    }

    private fun useCoinForProTimer(cost: Int, durationMillis: Long, type: String, prefs: android.content.SharedPreferences, description: String) {
        val currentBalance = loadCoinBalance()
        if (currentBalance < cost) {
            Toast.makeText(requireContext(), "Not enough coins!", Toast.LENGTH_SHORT).show()
            return
        }
        val endTime = System.currentTimeMillis() + durationMillis
        prefs.edit().putLong(KEY_PRO_TIMER_END, endTime).putString(KEY_PRO_TIMER_TYPE, type).apply()
        // Deduct coins and animate
        val newBalance = currentBalance - cost
        animateCoinBalance(currentBalance, newBalance)
        saveCoinBalance(newBalance)
        // Add to history
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        historyList.add(0, CoinHistory(HistoryType.SPENT, cost, today, description))
        updateDisplayedHistory()
        saveCoinHistory()
        Toast.makeText(requireContext(), "Pro timer started!", Toast.LENGTH_SHORT).show()
    }

    private fun animateCoinBalance(from: Int, to: Int) {
        val animator = android.animation.ValueAnimator.ofInt(from, to)
        animator.duration = 1000
        animator.addUpdateListener { valueAnimator ->
            binding.coinBalance.text = valueAnimator.animatedValue.toString()
        }
        animator.start()
    }

    private fun bounceView(view: View) {
        val animator = android.animation.ObjectAnimator.ofFloat(view, "translationY", 0f, -30f, 0f)
        animator.duration = 600
        animator.repeatCount = 2
        animator.start()
    }
}