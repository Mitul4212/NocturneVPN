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
    private val handler = Handler(Looper.getMainLooper())
    private var isAdRunning = false

    private var historyList = mutableListOf<CoinHistory>()
    private lateinit var adapter: CoinHistoryAdapter

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Load coin balance and history from SharedPreferences
        val savedBalance = loadCoinBalance()
        binding.coinBalance.text = savedBalance.toString()
        historyList = loadCoinHistory()
        adapter = CoinHistoryAdapter(historyList)
        binding.coinHistoryRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.coinHistoryRecyclerView.adapter = adapter
        updateHistoryVisibility()
        setupDailyCheckin()
        setupWatchAdSection()
        setupUseYourCoinSection()

        // Force update check-in UI with latest streak
        val prefs = requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val streak = prefs.getInt(KEY_STREAK, 0)
        val checkmarks = listOf(
            binding.icCheckDay1, binding.icCheckDay2, binding.icCheckDay3, binding.icCheckDay4,
            binding.icCheckDay5, binding.icCheckDay6, binding.icCheckDay7
        )
        updateCheckinUI(checkmarks, streak)
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

    private fun setupDailyCheckin() {
        val dayBtns = listOf(
            binding.day1Btn, binding.day2Btn, binding.day3Btn, binding.day4Btn,
            binding.day5Btn, binding.day6Btn, binding.day7Btn
        )
        val checkmarks = listOf(
            binding.icCheckDay1, binding.icCheckDay2, binding.icCheckDay3, binding.icCheckDay4,
            binding.icCheckDay5, binding.icCheckDay6, binding.icCheckDay7
        )
        val prefs = requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        // Always read the latest streak and lastDate from SharedPreferences
        val streak = prefs.getInt(KEY_STREAK, 0)
        val lastDate = prefs.getString(KEY_LAST_DATE, "")
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // Only allow check-in if today > last check-in date
        val canCheckInToday = lastDate != today
        val actualStreak = if (canCheckInToday) streak else streak - 1
        val safeStreak = actualStreak.coerceAtLeast(0)

        // Check if user broke streak (missed a day)
        if (lastDate != "" && lastDate != today) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val last = sdf.parse(lastDate)
            val now = sdf.parse(today)
            val diff = (now.time - last.time) / (1000 * 60 * 60 * 24)
            if (diff > 1) {
                // Reset streak
                prefs.edit().putInt(KEY_STREAK, 0).apply()
                updateCheckinUI(checkmarks, 0)
                setCheckinButtonsEnabled(dayBtns, 0, canCheckInToday)
                if (canCheckInToday) bounceView(dayBtns[0])
                return
            }
        }

        // Always update UI with the correct streak
        updateCheckinUI(checkmarks, safeStreak)
        setCheckinButtonsEnabled(dayBtns, safeStreak, canCheckInToday)
        if (canCheckInToday) bounceView(dayBtns[safeStreak])

        // Set click listener for current day only
        dayBtns.forEachIndexed { idx, btn ->
            btn.setOnClickListener {
                // Always re-read streak and lastDate before allowing check-in
                val currentPrefs = requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                val currentStreak = currentPrefs.getInt(KEY_STREAK, 0)
                val currentLastDate = currentPrefs.getString(KEY_LAST_DATE, "")
                val currentToday = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val canCheckIn = currentLastDate != currentToday && idx == (if (currentLastDate != currentToday) currentStreak else currentStreak - 1).coerceAtLeast(0)
                if (!canCheckIn || isAdRunning) return@setOnClickListener
                isAdRunning = true
                Toast.makeText(requireContext(), "Ad running...", Toast.LENGTH_LONG).show()
                handler.postDelayed({
                    // Mark as checked-in
                    val newStreak = currentStreak + 1
                    currentPrefs.edit().putInt(KEY_STREAK, newStreak).putString(KEY_LAST_DATE, currentToday).apply()
                    updateCheckinUI(checkmarks, newStreak)
                    setCheckinButtonsEnabled(dayBtns, newStreak, false)
                    isAdRunning = false
                    // --- Add reward to coinBalance and show in history ---
                    val previousBalance = loadCoinBalance()
                    val reward = 5 // or get from your reward logic
                    val newBalance = previousBalance + reward
                    animateCoinBalance(previousBalance, newBalance)
                    saveCoinBalance(newBalance)
                    // Add to history
                    historyList.add(0, CoinHistory(HistoryType.EARN, reward, currentToday, "Daily Check-in"))
                    adapter.notifyItemInserted(0)
                    binding.coinHistoryRecyclerView.scrollToPosition(0)
                    updateHistoryVisibility()
                    saveCoinHistory()
                }, 5000)
            }
        }
    }

    private fun updateCheckinUI(checkmarks: List<View>, streak: Int) {
        val dayBtns = listOf(
            binding.day1Btn, binding.day2Btn, binding.day3Btn, binding.day4Btn,
            binding.day5Btn, binding.day6Btn, binding.day7Btn
        )
        val checkinDays = listOf(
            binding.day1Btn.findViewById<TextView>(R.id.checkinDay1),
            binding.day2Btn.findViewById<TextView>(R.id.checkinDay2),
            binding.day3Btn.findViewById<TextView>(R.id.checkinDay3),
            binding.day4Btn.findViewById<TextView>(R.id.checkinDay4),
            binding.day5Btn.findViewById<TextView>(R.id.checkinDay5),
            binding.day6Btn.findViewById<TextView>(R.id.checkinDay6),
            binding.day7Btn.findViewById<TextView>(R.id.checkinDay7)
        )
        val checkinRewards = listOf(
            binding.day1Btn.findViewById<TextView>(R.id.checkinReward1),
            binding.day2Btn.findViewById<TextView>(R.id.checkinReward2),
            binding.day3Btn.findViewById<TextView>(R.id.checkinReward3),
            binding.day4Btn.findViewById<TextView>(R.id.checkinReward4),
            binding.day5Btn.findViewById<TextView>(R.id.checkinReward5),
            binding.day6Btn.findViewById<TextView>(R.id.checkinReward6),
            binding.day7Btn.findViewById<TextView>(R.id.checkinReward7)
        )
        checkmarks.forEachIndexed { idx, check ->
            check.isVisible = idx < streak
            if (idx < streak) {
                dayBtns[idx].setBackgroundResource(R.drawable.bg_checkedin_card)
                checkinDays[idx]?.setTextColor(resources.getColor(android.R.color.white, null))
                checkinRewards[idx]?.setTextColor(resources.getColor(android.R.color.white, null))
            } else {
                dayBtns[idx].setBackgroundResource(R.drawable.checkin_btn)
                checkinDays[idx]?.setTextColor(resources.getColor(R.color.gray, null))
                checkinRewards[idx]?.setTextColor(resources.getColor(R.color.gold, null))
            }
        }
    }

    private fun bounceView(view: View) {
        val animator = ObjectAnimator.ofFloat(view, "translationY", 0f, -30f, 0f)
        animator.duration = 600
        animator.repeatCount = 2
        animator.start()
    }

    private fun setCheckinButtonsEnabled(dayBtns: List<View>, streak: Int, canCheckInToday: Boolean) {
        dayBtns.forEachIndexed { idx, btn ->
            btn.isEnabled = idx == streak && canCheckInToday
        }
    }

    private fun animateCoinBalance(from: Int, to: Int) {
        val animator = ValueAnimator.ofInt(from, to)
        animator.duration = 1000
        animator.addUpdateListener { valueAnimator ->
            binding.coinBalance.text = valueAnimator.animatedValue.toString()
        }
        animator.start()
    }

    // Call this function whenever the user spends coins
    fun onUserSpentCoins(spentAmount: Int) {
        val currentBalance = binding.coinBalance.text.toString().replace(",", "").toIntOrNull() ?: 0
        val newBalance = (currentBalance - spentAmount).coerceAtLeast(0)
        animateCoinBalance(currentBalance, newBalance)
        // Optionally, update the stored balance after animation
    }

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
                adapter.notifyItemInserted(0)
                binding.coinHistoryRecyclerView.scrollToPosition(0)
                updateHistoryVisibility()
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
        adapter.notifyItemInserted(0)
        binding.coinHistoryRecyclerView.scrollToPosition(0)
        updateHistoryVisibility()
        saveCoinHistory()
        Toast.makeText(requireContext(), "Pro timer started!", Toast.LENGTH_SHORT).show()
    }
}