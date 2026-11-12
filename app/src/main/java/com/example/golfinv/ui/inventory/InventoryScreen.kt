package com.example.golfinv.ui.inventory

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.GolfCourse
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.example.golfinv.data.GolfClubEntity
import com.example.golfinv.ui.MainViewModel
import com.example.golfinv.ui.settings.BrandSettingsDialog
import com.example.golfinv.ui.theme.GolfAccentGold
import com.example.golfinv.ui.theme.GolfGreenPrimary
import com.example.golfinv.ui.theme.GolfGreenSecondary
import com.example.golfinv.utils.ExcelImporterExporter
import com.example.golfinv.utils.GolfStringUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InventoryScreen(
    viewModel: MainViewModel,
    clubs: List<GolfClubEntity>,
    searchQuery: String,
    selectedTab: Int,
    editingClub: GolfClubEntity?,
    onNavigateToScanner: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showAddManualDialog by rememberSaveable { mutableStateOf(false) }
    var clubToDelete by remember { mutableStateOf<GolfClubEntity?>(null) }
    var showTopSettingsMenu by remember { mutableStateOf(false) }
    var showBrandSettingsDialog by remember { mutableStateOf(false) }

    // Category Summary Dialog State (Opens when clicking any category button)
    var selectedCategoryForSummary by remember { mutableStateOf<String?>(null) }

    // Export Option Dialog States
    var showExportOptionDialog by remember { mutableStateOf(false) }
    var exportIncludePhotos by remember { mutableStateOf(true) }

    // Delete All Double Confirmation States
    var showDeleteAllStep1Dialog by remember { mutableStateOf(false) }
    var showDeleteAllStep2Dialog by remember { mutableStateOf(false) }
    var deleteConfirmationInputText by remember { mutableStateOf("") }

    // Category Filter State
    var selectedCategoryFilter by rememberSaveable { mutableStateOf("Keseluruhan") }
    val categoryOptions = listOf("Keseluruhan", "Driver", "Fairway Wood", "Rescue", "Iron", "Wedge", "Putter")

    val brandList by viewModel.brandList.collectAsStateWithLifecycle()

    // SAF Launchers for Export & Import Excel (.xls) with direct embedded Base64 photos
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.ms-excel")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val success = ExcelImporterExporter.exportToExcelXls(context, uri, clubs, exportIncludePhotos)
                if (success) {
                    val modeStr = if (exportIncludePhotos) "beserta foto" else "tanpa foto"
                    Toast.makeText(context, "Berhasil mengekspor ${clubs.size} stik golf ($modeStr) ke file Excel (.xls)!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Gagal mengekspor file Excel.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val imported = ExcelImporterExporter.importFromExcelXls(context, uri)
                if (imported.isNotEmpty()) {
                    viewModel.importClubs(imported)
                    Toast.makeText(context, "Berhasil mengimpor ${imported.size} stik golf beserta foto dari Excel!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Tidak ada data stik golf yang ditemukan di file.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Filter into Available (Tersedia) and Sold (Terjual)
    val availableClubs = remember(clubs) { clubs.filter { !it.isSold } }
    val soldClubs = remember(clubs) { clubs.filter { it.isSold } }

    val currentTabClubs = if (selectedTab == 0) availableClubs else soldClubs

    // Filter by Category (Keseluruhan, Driver, Iron, Wedge, Putter, etc.)
    val categoryFilteredClubs = remember(currentTabClubs, selectedCategoryFilter) {
        if (selectedCategoryFilter == "Keseluruhan") {
            currentTabClubs
        } else {
            currentTabClubs.filter { it.clubType.equals(selectedCategoryFilter, ignoreCase = true) }
        }
    }

    // Filter by Search Query
    val filteredClubs = remember(categoryFilteredClubs, searchQuery) {
        if (searchQuery.isBlank()) {
            categoryFilteredClubs
        } else {
            categoryFilteredClubs.filter {
                it.brand.contains(searchQuery, ignoreCase = true) ||
                        it.clubType.contains(searchQuery, ignoreCase = true) ||
                        it.modelType.contains(searchQuery, ignoreCase = true) ||
                        it.loft.contains(searchQuery, ignoreCase = true) ||
                        it.shaft.contains(searchQuery, ignoreCase = true) ||
                        it.grip.contains(searchQuery, ignoreCase = true) ||
                        it.releaseYear.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Fullscreen LazyColumn: Top headers scroll away so the list becomes FULLSCREEN when scrolling
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Item 1: Title Header & Settings Overflow Menu (Titik Tiga ⋮)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Inventory Stik Golf",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${availableClubs.size} Tersedia • ${soldClubs.size} Terjual",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = GolfGreenPrimary.copy(alpha = 0.1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GolfCourse,
                                contentDescription = null,
                                tint = GolfGreenPrimary,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Top Settings Overflow Menu Button (Titik Tiga ⋮)
                        Box {
                            IconButton(onClick = { showTopSettingsMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Pengaturan Inventory",
                                    tint = GolfGreenPrimary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showTopSettingsMenu,
                                onDismissRequest = { showTopSettingsMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("🏷️ Kelola Merek Stik") },
                                    onClick = {
                                        showTopSettingsMenu = false
                                        showBrandSettingsDialog = true
                                    }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("📊 Export Excel (.xls)") },
                                    onClick = {
                                        showTopSettingsMenu = false
                                        showExportOptionDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("📥 Import Excel (.xls)") },
                                    onClick = {
                                        showTopSettingsMenu = false
                                        importLauncher.launch(arrayOf("*/*", "application/vnd.ms-excel", "text/csv"))
                                    }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("🚨 Hapus Semua List", color = Color.Red, fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        showTopSettingsMenu = false
                                        showDeleteAllStep1Dialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Item 2: Category Filter Buttons (Clicking opens popup summary with items, rupiah, and brand breakdown)
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "KATEGORI STIK GOLF (KLIK UNTUK RINGKASAN)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GolfGreenSecondary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categoryOptions.forEach { category ->
                            val isSelected = selectedCategoryFilter == category
                            val count = if (category == "Keseluruhan") {
                                currentTabClubs.size
                            } else {
                                currentTabClubs.count { it.clubType.equals(category, ignoreCase = true) }
                            }

                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    // Clicking opens the summary popup showing item count, rupiah, and brand breakdown
                                    selectedCategoryForSummary = category
                                },
                                label = { Text("$category ($count)", fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GolfGreenPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // Sticky Header: Segmented Tabs & Search Bar stick cleanly at the top when scrolling
            stickyHeader {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(vertical = 4.dp)
                ) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = GolfGreenPrimary,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = GolfGreenPrimary
                            )
                        }
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { viewModel.setSelectedInventoryTab(0) },
                            text = {
                                Text(
                                    text = "⛳ Tersedia (${availableClubs.size})",
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp
                                )
                            }
                        )

                        Tab(
                            selected = selectedTab == 1,
                            onClick = { viewModel.setSelectedInventoryTab(1) },
                            text = {
                                Text(
                                    text = "🤝 Terjual (${soldClubs.size})",
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp
                                )
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Search...") },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Search, contentDescription = null)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true
                        )

                        OutlinedButton(
                            onClick = { showAddManualDialog = true },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.height(56.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Manual", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (filteredClubs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = GolfGreenPrimary.copy(alpha = 0.1f),
                                modifier = Modifier.size(72.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.GolfCourse,
                                        contentDescription = null,
                                        tint = GolfGreenPrimary,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (selectedTab == 0) "Belum Ada Stik Golf Kategori Ini" else "Belum Ada Stik Golf Terjual Kategori Ini",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Gunakan kategori lain atau tambahkan stik baru.",
                                fontSize = 13.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            } else {
                // List of Inventory Cards taking FULLSCREEN when scrolled down
                items(filteredClubs, key = { it.id }) { club ->
                    GolfClubCard(
                        club = club,
                        onEdit = { viewModel.setEditingClub(club) },
                        onDelete = { clubToDelete = club },
                        onToggleSold = { isSold -> viewModel.toggleClubSoldStatus(club, isSold) },
                        onCopyText = {
                            val text = GolfStringUtils.formatClubAsText(club)
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Stik Golf", text)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Text stik golf disalin ke clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        onShareText = {
                            val text = GolfStringUtils.formatClubAsText(club)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            context.startActivity(Intent.createChooser(intent, "Bagikan Data Stik Golf"))
                        }
                    )
                }
            }
        }

        // Primary Action: Camera Scanner FAB
        FloatingActionButton(
            onClick = { onNavigateToScanner() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = GolfGreenPrimary,
            contentColor = Color.White
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, tint = GolfAccentGold)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Scan Stik Golf", fontWeight = FontWeight.Bold)
            }
        }
    }

    // Category Summary Popup Dialog (Shows Item Count, Rupiah, and Brand breakdown sorted descending)
    if (selectedCategoryForSummary != null) {
        CategorySummaryDialog(
            categoryName = selectedCategoryForSummary!!,
            clubs = currentTabClubs,
            onDismiss = { selectedCategoryForSummary = null },
            onFilterHere = {
                selectedCategoryFilter = selectedCategoryForSummary!!
                selectedCategoryForSummary = null
            }
        )
    }

    // Brand Management Dialog
    if (showBrandSettingsDialog) {
        BrandSettingsDialog(
            brands = brandList,
            onAddBrand = { viewModel.addBrand(it) },
            onUpdateBrand = { brand, newName -> viewModel.updateBrand(brand, newName) },
            onDeleteBrand = { viewModel.deleteBrand(it) },
            onImportBrands = { importedBrands -> viewModel.importBrands(importedBrands, replaceExisting = true) },
            onDismiss = { showBrandSettingsDialog = false }
        )
    }

    // Export Option Dialog (With or Without Photos)
    if (showExportOptionDialog) {
        AlertDialog(
            onDismissRequest = { showExportOptionDialog = false },
            title = { Text("📊 Export Excel (.xls)", fontWeight = FontWeight.Bold) },
            text = { Text("Apakah Anda ingin menyertakan foto cropped head stik golf ke dalam file Excel (.xls)?") },
            confirmButton = {
                Button(
                    onClick = {
                        showExportOptionDialog = false
                        exportIncludePhotos = true
                        exportLauncher.launch("Golf_Inventory_Export_${System.currentTimeMillis()}.xls")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GolfGreenPrimary)
                ) {
                    Text("📷 Ya, Sertakan Foto")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            showExportOptionDialog = false
                            exportIncludePhotos = false
                            exportLauncher.launch("Golf_Inventory_Export_${System.currentTimeMillis()}.xls")
                        }
                    ) {
                        Text("Tanpa Foto")
                    }
                    OutlinedButton(onClick = { showExportOptionDialog = false }) {
                        Text("Back")
                    }
                }
            }
        )
    }

    // Delete Single Item Confirmation Dialog
    if (clubToDelete != null) {
        AlertDialog(
            onDismissRequest = { clubToDelete = null },
            title = { Text("Hapus Stik Golf?", fontWeight = FontWeight.Bold) },
            text = { Text("Apakah Anda yakin ingin menghapus stik golf ini dari inventory? Tindakan ini tidak dapat dibatalkan.") },
            confirmButton = {
                Button(
                    onClick = {
                        val target = clubToDelete
                        clubToDelete = null
                        if (target != null) {
                            viewModel.deleteClub(target)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { clubToDelete = null }) {
                    Text("Back")
                }
            }
        )
    }

    // Delete ALL List Double Confirmation - Step 1
    if (showDeleteAllStep1Dialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllStep1Dialog = false },
            title = { Text("⚠️ Hapus Seluruh List Inventory? (Langkah 1/2)", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
            text = { Text("Apakah Anda yakin ingin menghapus SELURUH ${clubs.size} stik golf dari database inventory?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAllStep1Dialog = false
                        deleteConfirmationInputText = ""
                        showDeleteAllStep2Dialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Lanjut Konfirmasi Akhir")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteAllStep1Dialog = false }) {
                    Text("Back")
                }
            }
        )
    }

    // Delete ALL List Double Confirmation - Step 2 (Requires typing "HAPUS")
    if (showDeleteAllStep2Dialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllStep2Dialog = false },
            title = { Text("🚨 KONFIRMASI PERMANEN - HAPUS SEMUA (2/2)", fontWeight = FontWeight.Bold, color = Color.Red, fontSize = 16.sp) },
            text = {
                Column {
                    Text("Ketik kata 'HAPUS' di bawah ini untuk mengonfirmasi penghapusan seluruh ${clubs.size} stik golf secara permanen.")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = deleteConfirmationInputText,
                        onValueChange = { deleteConfirmationInputText = it },
                        placeholder = { Text("Ketik: HAPUS") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                val isConfirmationValid = deleteConfirmationInputText.trim().equals("HAPUS", ignoreCase = true)
                Button(
                    onClick = {
                        showDeleteAllStep2Dialog = false
                        viewModel.deleteAllClubs()
                        Toast.makeText(context, "Seluruh list inventory telah dihapus.", Toast.LENGTH_LONG).show()
                    },
                    enabled = isConfirmationValid,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red,
                        disabledContainerColor = Color.Red.copy(alpha = 0.3f)
                    )
                ) {
                    Text("EKSEKUSI HAPUS PERMANEN")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteAllStep2Dialog = false }) {
                    Text("Back")
                }
            }
        )
    }

    // Dialog for Editing existing item
    if (editingClub != null) {
        EditGolfClubDialog(
            club = editingClub,
            isNewItem = false,
            brandList = brandList,
            onDismiss = { viewModel.setEditingClub(null) },
            onSave = { brand, clubType, type, loft, isAdjustable, year, shaft, grip, condition, cost, sell, isSold, notes ->
                viewModel.updateClub(
                    editingClub.copy(
                        brand = brand,
                        clubType = clubType,
                        modelType = type,
                        loft = loft,
                        isAdjustable = isAdjustable,
                        releaseYear = year,
                        shaft = shaft,
                        grip = grip,
                        conditionPercent = condition,
                        costPrice = cost,
                        sellingPrice = sell,
                        isSold = isSold,
                        soldDate = if (isSold) (editingClub.soldDate ?: System.currentTimeMillis()) else null,
                        notes = notes
                    )
                )
            },
            onDelete = {
                val target = editingClub
                viewModel.setEditingClub(null)
                clubToDelete = target
            }
        )
    }

    // Dialog for Adding manual item
    if (showAddManualDialog) {
        EditGolfClubDialog(
            club = null,
            isNewItem = true,
            brandList = brandList,
            onDismiss = { showAddManualDialog = false },
            onSave = { brand, clubType, type, loft, isAdjustable, year, shaft, grip, condition, cost, sell, isSold, notes ->
                viewModel.saveManualClub(
                    brand, clubType, type, loft, isAdjustable, year, shaft, grip, condition, cost, sell, isSold, notes
                )
                showAddManualDialog = false
            }
        )
    }
}

/**
 * Category Summary Popup Dialog showing total item count, rupiah financials,
 * and brand breakdown sorted from most to least (count >= 1).
 */
@Composable
fun CategorySummaryDialog(
    categoryName: String,
    clubs: List<GolfClubEntity>,
    onDismiss: () -> Unit,
    onFilterHere: () -> Unit
) {
    val categoryClubs = if (categoryName == "Keseluruhan") {
        clubs
    } else {
        clubs.filter { it.clubType.equals(categoryName, ignoreCase = true) }
    }

    val itemCount = categoryClubs.size
    val totalSelling = categoryClubs.sumOf { it.sellingPrice }
    val totalCost = categoryClubs.sumOf { it.costPrice }
    val totalProfit = (totalSelling - totalCost)

    // Group by brand, count items, filter >= 1, and sort descending (terbanyak ke tersedikit)
    val brandBreakdown = remember(categoryClubs) {
        categoryClubs
            .groupBy { it.brand }
            .mapValues { entry -> entry.value.size }
            .filter { it.value >= 1 }
            .entries
            .sortedByDescending { it.value }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = GolfGreenPrimary
                ) {
                    Icon(
                        imageVector = Icons.Default.GolfCourse,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .padding(6.dp)
                            .size(20.dp)
                    )
                }
                Text(
                    text = "Ringkasan: $categoryName",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Item Count Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = GolfGreenPrimary.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Jumlah Item:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("$itemCount Stik Golf", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = GolfGreenPrimary)
                    }
                }

                // Financial Summary Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = GolfGreenPrimary)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("TOTAL HARGA JUAL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f))
                        Text(GolfStringUtils.formatPrice(totalSelling), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Total Modal", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                                Text(GolfStringUtils.formatPrice(totalCost), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Potensi Keuntungan", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                                Text(GolfStringUtils.formatPrice(totalProfit), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GolfAccentGold)
                            }
                        }
                    }
                }

                // Brand Breakdown Section (Sorted descending, count >= 1)
                if (brandBreakdown.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "JUMLAH MEREK (URUT TERBANYAK)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GolfGreenSecondary,
                        letterSpacing = 0.5.sp
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            brandBreakdown.forEach { (brandName, count) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = brandName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF0F172A)
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = GolfGreenPrimary.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = "$count item",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GolfGreenPrimary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onFilterHere()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = GolfGreenPrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Tampilkan List", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
                Text("Tutup")
            }
        }
    )
}

/**
 * Clean Inventory Card matching the mockup design in inv.JPG:
 * - Rounded white card with thin border (16.dp corners)
 * - Left: Square cropped image thumbnail (68.dp x 68.dp) with 10.dp corners
 * - Middle: Title in bold, Subtitle with Chevron (> ClubType • Year), Metadata rows with icons, Price & Status
 * - Right: 3-dots overflow menu (⋮) with Edit, Copy, Share, Toggle Sold, and Delete actions
 */
@Composable
fun GolfClubCard(
    club: GolfClubEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleSold: (Boolean) -> Unit,
    onCopyText: () -> Unit,
    onShareText: () -> Unit
) {
    var showOverflowMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
            .clickable { onEdit() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Square Thumbnail (68.dp x 68.dp)
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF1F5F9)),
                contentAlignment = Alignment.Center
            ) {
                if (!club.imagePath.isNullOrBlank()) {
                    Image(
                        painter = rememberAsyncImagePainter(model = club.imagePath),
                        contentDescription = "Thumbnail Head",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.GolfCourse,
                        contentDescription = null,
                        tint = GolfGreenPrimary,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Middle: Information Details (Matching inv.JPG layout)
            Column(modifier = Modifier.weight(1f)) {
                // Line 1: Title (Brand + Model) in Bold Text
                Text(
                    text = "${club.brand} ${club.modelType}".trim(),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Line 2: Category line with Chevron (> ClubType • Th. ReleaseYear)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = GolfGreenPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "${club.clubType} • Th. ${club.releaseYear}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GolfGreenPrimary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Line 3: Metadata Row with Icons (Layers Icon for Loft / Adjustable, Tag for Condition)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Loft Degree & Adjustable Status
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${club.loft} ${if (club.isAdjustable) "(Adjustable)" else "(Fixed)"}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF475569)
                        )
                    }

                    // Condition (%)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Sell,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${club.conditionPercent}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF16A34A)
                        )
                    }
                }

                // Line 4: Pricing Row (if set)
                if (club.sellingPrice > 0 || club.costPrice > 0) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (club.sellingPrice > 0) "Jual: ${GolfStringUtils.formatPrice(club.sellingPrice)}" else "Modal: ${GolfStringUtils.formatPrice(club.costPrice)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        if (club.sellingPrice > 0 && club.costPrice > 0) {
                            Text(
                                text = " (Modal: ${GolfStringUtils.formatPrice(club.costPrice)})",
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                // Line 5: Clock Icon + Status Timestamp line (Matching inv.JPG bottom line)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (club.isSold) "Status: Terjual 🤝" else "Status: Tersedia ⛳",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (club.isSold) Color(0xFFDC2626) else Color(0xFF64748B)
                    )
                }
            }

            // Right: 3-Dots Vertical Overflow Menu Button (⋮)
            Box {
                IconButton(
                    onClick = { showOverflowMenu = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Opsi Stik",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = showOverflowMenu,
                    onDismissRequest = { showOverflowMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("✏️ Edit Detail") },
                        onClick = {
                            showOverflowMenu = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("📋 Salin Text") },
                        onClick = {
                            showOverflowMenu = false
                            onCopyText()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("📤 Bagikan") },
                        onClick = {
                            showOverflowMenu = false
                            onShareText()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (club.isSold) "⛳ Tandai Tersedia" else "🤝 Tandai Terjual") },
                        onClick = {
                            showOverflowMenu = false
                            onToggleSold(!club.isSold)
                        }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("🗑️ Hapus", color = Color.Red) },
                        onClick = {
                            showOverflowMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}
