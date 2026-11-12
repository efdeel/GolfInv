package com.example.golfinv.ui

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.golfinv.data.BrandEntity
import com.example.golfinv.data.GolfClubEntity
import com.example.golfinv.data.GolfRepository
import com.example.golfinv.recognition.GolfClubRecognitionEngine
import com.example.golfinv.recognition.IdentifiedGolfClub
import com.example.golfinv.utils.ImageUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ScannerState(
    val isLocked: Boolean = false,
    val lockedBitmap: Bitmap? = null,
    val isAnalyzing: Boolean = false,
    val identifiedClub: IdentifiedGolfClub? = null,
    val matchedInventoryClub: GolfClubEntity? = null,
    val savedSuccessfully: Boolean = false
)

class MainViewModel(
    private val repository: GolfRepository
) : ViewModel() {

    init {
        viewModelScope.launch {
            repository.ensureDefaultBrandsSeeded()
        }
    }

    val inventoryList: StateFlow<List<GolfClubEntity>> = repository.allClubs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val brandList: StateFlow<List<BrandEntity>> = repository.allBrands
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _scannerState = MutableStateFlow(ScannerState())
    val scannerState: StateFlow<ScannerState> = _scannerState.asStateFlow()

    private val _editingClub = MutableStateFlow<GolfClubEntity?>(null)
    val editingClub: StateFlow<GolfClubEntity?> = _editingClub.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedInventoryTab = MutableStateFlow(0) // 0 = Available, 1 = Sold
    val selectedInventoryTab: StateFlow<Int> = _selectedInventoryTab.asStateFlow()

    fun setSelectedInventoryTab(tabIndex: Int) {
        _selectedInventoryTab.value = tabIndex
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addBrand(name: String) {
        viewModelScope.launch {
            repository.insertBrand(name)
        }
    }

    fun updateBrand(brand: BrandEntity, newName: String) {
        viewModelScope.launch {
            repository.updateBrand(brand, newName)
        }
    }

    fun deleteBrand(brand: BrandEntity) {
        viewModelScope.launch {
            repository.deleteBrand(brand)
        }
    }

    fun importBrands(brands: List<BrandEntity>, replaceExisting: Boolean = true) {
        viewModelScope.launch {
            repository.importBrands(brands, replaceExisting)
        }
    }

    fun deleteAllBrands() {
        viewModelScope.launch {
            repository.deleteAllBrands()
        }
    }

    /**
     * Captures camera bitmap, crops it strictly to the yellow reticle frame box,
     * and performs OCR + background web visual comparison & spec extraction.
     */
    fun captureHeadAndAnalyze(fullBitmap: Bitmap) {
        val croppedBitmap = ImageUtils.cropToYellowReticleBox(fullBitmap)

        _scannerState.value = ScannerState(
            isLocked = true,
            lockedBitmap = croppedBitmap,
            isAnalyzing = true,
            identifiedClub = null,
            matchedInventoryClub = null,
            savedSuccessfully = false
        )

        viewModelScope.launch {
            val currentBrands = brandList.value.map { data -> data.name }
            val result = GolfClubRecognitionEngine.processImage(croppedBitmap, currentBrands)

            val allClubs = inventoryList.value
            val bestInventoryMatch = allClubs.find { club ->
                club.brand.equals(result.brand, ignoreCase = true) &&
                        club.modelType.contains(result.modelType, ignoreCase = true)
            } ?: allClubs.firstOrNull { club ->
                club.brand.equals(result.brand, ignoreCase = true)
            }

            _scannerState.value = _scannerState.value.copy(
                isAnalyzing = false,
                identifiedClub = result,
                matchedInventoryClub = bestInventoryMatch
            )
        }
    }

    fun lockHeadAndAnalyze(bitmap: Bitmap) = captureHeadAndAnalyze(bitmap)

    fun unlockHead() {
        _scannerState.value = ScannerState(
            isLocked = false,
            lockedBitmap = null,
            isAnalyzing = false,
            identifiedClub = null,
            matchedInventoryClub = null,
            savedSuccessfully = false
        )
    }

    fun updateIdentifiedClub(updated: IdentifiedGolfClub) {
        _scannerState.value = _scannerState.value.copy(
            identifiedClub = updated
        )
    }

    fun saveIdentifiedClubToInventory(
        customBrand: String? = null,
        customClubType: String? = null,
        customType: String? = null,
        customLoft: String? = null,
        customIsAdjustable: Boolean? = null,
        customReleaseYear: String? = null,
        shaft: String = "",
        grip: String = "",
        conditionPercent: Int = 90,
        costPrice: Long = 0,
        sellingPrice: Long = 0,
        isSold: Boolean = false,
        notes: String = ""
    ) {
        val currentState = _scannerState.value
        val identified = currentState.identifiedClub ?: return
        val croppedBitmap = currentState.lockedBitmap

        viewModelScope.launch {
            val imagePath = croppedBitmap?.let { repository.saveHeadBitmap(it) }

            val entity = GolfClubEntity(
                brand = customBrand ?: identified.brand,
                clubType = customClubType ?: identified.clubType,
                modelType = customType ?: identified.modelType,
                loft = customLoft ?: identified.loft,
                isAdjustable = customIsAdjustable ?: identified.isAdjustable,
                releaseYear = customReleaseYear ?: identified.releaseYear,
                shaft = shaft,
                grip = grip,
                conditionPercent = conditionPercent,
                costPrice = costPrice,
                sellingPrice = sellingPrice,
                isSold = isSold,
                soldDate = if (isSold) System.currentTimeMillis() else null,
                notes = notes,
                imagePath = imagePath
            )

            repository.insertClub(entity)
            _scannerState.value = _scannerState.value.copy(savedSuccessfully = true)
        }
    }

    fun saveManualClub(
        brand: String,
        clubType: String,
        modelType: String,
        loft: String,
        isAdjustable: Boolean,
        releaseYear: String,
        shaft: String,
        grip: String,
        conditionPercent: Int,
        costPrice: Long,
        sellingPrice: Long,
        isSold: Boolean,
        notes: String
    ) {
        viewModelScope.launch {
            val entity = GolfClubEntity(
                brand = brand.ifBlank { "No Brand" },
                clubType = clubType.ifBlank { "Driver" },
                modelType = modelType.ifBlank { "Golf Club" },
                loft = loft.ifBlank { "10.5°" },
                isAdjustable = isAdjustable,
                releaseYear = releaseYear.ifBlank { "2024" },
                shaft = shaft,
                grip = grip,
                conditionPercent = conditionPercent,
                costPrice = costPrice,
                sellingPrice = sellingPrice,
                isSold = isSold,
                soldDate = if (isSold) System.currentTimeMillis() else null,
                notes = notes,
                imagePath = null
            )
            repository.insertClub(entity)
        }
    }

    fun importClubs(clubs: List<GolfClubEntity>) {
        viewModelScope.launch {
            repository.insertClubs(clubs)
        }
    }

    fun toggleClubSoldStatus(club: GolfClubEntity, isSold: Boolean) {
        viewModelScope.launch {
            val updated = club.copy(
                isSold = isSold,
                soldDate = if (isSold) System.currentTimeMillis() else null
            )
            repository.updateClub(updated)
        }
    }

    fun setEditingClub(club: GolfClubEntity?) {
        _editingClub.value = club
    }

    fun updateClub(club: GolfClubEntity) {
        viewModelScope.launch {
            repository.updateClub(club)
            _editingClub.value = null
        }
    }

    fun deleteClub(club: GolfClubEntity) {
        viewModelScope.launch {
            repository.deleteClub(club)
            if (_editingClub.value?.id == club.id) {
                _editingClub.value = null
            }
        }
    }

    fun deleteAllClubs() {
        viewModelScope.launch {
            repository.deleteAllClubs()
            _editingClub.value = null
        }
    }
}

class MainViewModelFactory(
    private val repository: GolfRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
