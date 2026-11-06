package com.example.system_mini_capstone.admin

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.system_mini_capstone.admin.analytics.AnalyticsRepository
import com.example.system_mini_capstone.admin.analytics.AnalyticsRepositoryImpl
import com.example.system_mini_capstone.admin.analytics.DailyActivityStat
import com.example.system_mini_capstone.admin.analytics.GlobalMetrics
import com.example.system_mini_capstone.admin.analytics.LessonPerformance
import com.example.system_mini_capstone.admin.analytics.PdfReportGenerator
import com.example.system_mini_capstone.data.SessionManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class AnalyticsUiState(
    val metrics: GlobalMetrics = GlobalMetrics(0, 0, 0, 0f, 0, 0f),
    val topLessons: List<LessonPerformance> = emptyList(),
    val lowLessons: List<LessonPerformance> = emptyList(),
    val weeklyStats: List<DailyActivityStat> = emptyList(),
    val selectedTab: Int = 0, // 0 = Top Performing, 1 = Low Performing
    val isLoading: Boolean = false,
    val isExportingPdf: Boolean = false,
    val exportedPdfFile: File? = null,
    val statusMessage: String? = null
)

sealed interface AnalyticsEvent {
    data class PdfExported(val file: File) : AnalyticsEvent
    data class Error(val message: String) : AnalyticsEvent
}

class AdminAnalyticsViewModel(
    application: Application,
    private val repository: AnalyticsRepository = AnalyticsRepositoryImpl()
) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<AnalyticsEvent>()
    val eventFlow: SharedFlow<AnalyticsEvent> = _eventFlow.asSharedFlow()

    init {
        loadAnalyticsData()
    }

    fun loadAnalyticsData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val metrics = repository.queryGlobalMetrics()
                val topLessons = repository.queryTopPerformingLessons(5)
                val lowLessons = repository.queryLowPerformingLessons(5)
                val weeklyStats = repository.queryWeeklyActivity()

                _uiState.update {
                    it.copy(
                        metrics = metrics,
                        topLessons = topLessons,
                        lowLessons = lowLessons,
                        weeklyStats = weeklyStats,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        statusMessage = "Failed to load metrics: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun selectContentTab(tabIndex: Int) {
        _uiState.update { it.copy(selectedTab = tabIndex) }
    }

    fun exportPdf(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExportingPdf = true) }
            try {
                val adminEmail = sessionManager.getUserEmail() ?: "admin@signlink.app"
                val currentState = _uiState.value

                val pdfFile = PdfReportGenerator.generateReport(
                    context = context,
                    adminEmail = adminEmail,
                    metrics = currentState.metrics,
                    topLessons = currentState.topLessons,
                    lowLessons = currentState.lowLessons,
                    weeklyStats = currentState.weeklyStats
                )

                _uiState.update {
                    it.copy(
                        isExportingPdf = false,
                        exportedPdfFile = pdfFile,
                        statusMessage = "PDF Report generated: ${pdfFile.name}"
                    )
                }
                _eventFlow.emit(AnalyticsEvent.PdfExported(pdfFile))
            } catch (e: Exception) {
                val error = "PDF Generation failed: ${e.localizedMessage ?: "Unknown error"}"
                _uiState.update { it.copy(isExportingPdf = false, statusMessage = error) }
                _eventFlow.emit(AnalyticsEvent.Error(error))
            }
        }
    }

    fun dismissStatus() {
        _uiState.update { it.copy(statusMessage = null) }
    }
}
