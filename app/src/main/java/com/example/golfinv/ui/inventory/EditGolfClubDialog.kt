package com.example.golfinv.ui.inventory

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.rememberAsyncImagePainter
import com.example.golfinv.data.BrandEntity
import com.example.golfinv.data.GolfClubEntity
import com.example.golfinv.recognition.GolfClubRecognitionEngine
import com.example.golfinv.ui.theme.GolfAccentGold
import com.example.golfinv.ui.theme.GolfGreenPrimary
import com.example.golfinv.ui.theme.GolfGreenSecondary

@Composable
fun EditGolfClubDialog(
    club: GolfClubEntity?,
    isNewItem: Boolean = false,
    brandList: List<BrandEntity> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (
        brand: String,
        clubType: String,
        type: String,
        loft: String,
        isAdjustable: Boolean,
        year: String,
        shaft: String,
        grip: String,
        conditionPercent: Int,
        costPrice: Long,
        sellingPrice: Long,
        isSold: Boolean,
        notes: String
    ) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val clubId = club?.id ?: -1L

    var brand by remember(clubId, isNewItem) { mutableStateOf(club?.brand ?: "") }
    var clubType by remember(clubId, isNewItem) { mutableStateOf(club?.clubType ?: "Driver") }
    var type by remember(clubId, isNewItem) { mutableStateOf(club?.modelType ?: "") }
    var loft by remember(clubId, isNewItem) { mutableStateOf(club?.loft ?: "10.5°") }
    var isAdjustable by remember(clubId, isNewItem) { mutableStateOf(club?.isAdjustable ?: true) }
    var releaseYear by remember(clubId, isNewItem) { mutableStateOf(club?.releaseYear ?: "2024") }
    var shaft by remember(clubId, isNewItem) { mutableStateOf(club?.shaft ?: "") }
    var grip by remember(clubId, isNewItem) { mutableStateOf(club?.grip ?: "") }

    val initialCondition = (club?.conditionPercent ?: 90).coerceIn(10, 100).toFloat()
    var conditionPercentSlider by remember(clubId, isNewItem) { mutableFloatStateOf(initialCondition) }

    var costPriceText by remember(clubId, isNewItem) { mutableStateOf(if ((club?.costPrice ?: 0) > 0) club?.costPrice.toString() else "") }
    var sellingPriceText by remember(clubId, isNewItem) { mutableStateOf(if ((club?.sellingPrice ?: 0) > 0) club?.sellingPrice.toString() else "") }
    var isSold by remember(clubId, isNewItem) { mutableStateOf(club?.isSold ?: false) }
    var notes by remember(clubId, isNewItem) { mutableStateOf(club?.notes ?: "") }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showBrandPicker by remember { mutableStateOf(false) }
    var showFullScreenImage by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = GolfGreenPrimary,
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Text(
                        text = if (isNewItem) "+" else "✏️",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = if (isNewItem) "Tambah Stik Golf Baru" else "Edit Detail Stik Golf",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Photo Thumbnail Preview (Clickable to view almost full-screen with pinch-to-zoom)
                if (!club?.imagePath.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF1F5F9))
                            .clickable { showFullScreenImage = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(model = club?.imagePath),
                            contentDescription = "Foto Head Stik Golf",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black.copy(alpha = 0.65f),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "🔍 Tap Fullscreen",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Section 1: Kategori & Merek
                SectionHeader("🏌️ KATEGORI & MODEL")
                Spacer(modifier = Modifier.height(6.dp))

                Text("Club Type", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = GolfGreenPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    GolfClubRecognitionEngine.clubTypeOptions.forEach { typeOption ->
                        val isSelected = clubType.equals(typeOption, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { clubType = typeOption },
                            label = { Text(typeOption, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GolfGreenPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Brand Selector Field (Clickable to open Brand Picker Dialog)
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showBrandPicker = true }) {
                    OutlinedTextField(
                        value = brand,
                        onValueChange = { brand = it },
                        label = { Text("Brand / Merek (Pilih dari List / Ketik)") },
                        placeholder = { Text("Klik ikon dropdown untuk pilih merek...") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = true,
                        trailingIcon = {
                            IconButton(onClick = { showBrandPicker = true }) {
                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Pilih Merek")
                            }
                        },
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = type,
                    onValueChange = { type = it },
                    label = { Text("Model / Tipe Stik") },
                    placeholder = { Text("Qi10 Max, TSR3, G430 MAX") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                // Section 2: Spesifikasi Teknis
                SectionHeader("📐 SPESIFIKASI TEKNIS")
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = loft,
                        onValueChange = { loft = it },
                        label = { Text("Loft Degree") },
                        placeholder = { Text("10.5°") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = releaseYear,
                        onValueChange = { releaseYear = it },
                        label = { Text("Tahun Release") },
                        placeholder = { Text("2024") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = shaft,
                    onValueChange = { shaft = it },
                    label = { Text("Shaft (Opsional)") },
                    placeholder = { Text("Ventus Blue 6S, KBS Tour") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = grip,
                    onValueChange = { grip = it },
                    label = { Text("Grip (Opsional)") },
                    placeholder = { Text("Golf Pride MCC Plus4") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF4F7F5), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Terdapat Sleeve (Adjustable Loft)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (isAdjustable) "Hosel/Baut dapat disetel (Adjustable)" else "Fixed / Bonded Hosel",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = isAdjustable,
                        onCheckedChange = { isAdjustable = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = GolfGreenPrimary, checkedTrackColor = GolfGreenPrimary.copy(alpha = 0.3f))
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                // Section 3: Kondisi & Keuangan
                SectionHeader("📊 KONDISI & HARGA")
                Spacer(modifier = Modifier.height(6.dp))

                // Condition Slider with Safe bounds
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Kondisi Stik (%)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = GolfGreenPrimary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "${conditionPercentSlider.toInt()}%",
                            fontWeight = FontWeight.Bold,
                            color = GolfGreenPrimary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                Slider(
                    value = conditionPercentSlider.coerceIn(10f, 100f),
                    onValueChange = { conditionPercentSlider = it },
                    valueRange = 10f..100f,
                    colors = SliderDefaults.colors(thumbColor = GolfGreenPrimary, activeTrackColor = GolfGreenPrimary),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Pricing Inputs: Modal & Harga Jual
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = costPriceText,
                        onValueChange = { costPriceText = it.filter { char -> char.isDigit() } },
                        label = { Text("Modal Beli (Rp)") },
                        placeholder = { Text("3500000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = sellingPriceText,
                        onValueChange = { sellingPriceText = it.filter { char -> char.isDigit() } },
                        label = { Text("Harga Jual (Rp)") },
                        placeholder = { Text("4500000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Sold Switch Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isSold) Color(0xFFFFEBEE) else Color(0xFFF4F7F5), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isSold) "Status: TERJUAL (Sold Out) 🤝" else "Status: TERSEDIA (Available) ⛳",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isSold) Color.Red else GolfGreenPrimary
                        )
                        Text(
                            text = if (isSold) "Pindahkan ke tab daftar Terjual" else "Stik masih tersedia di inventory",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = isSold,
                        onCheckedChange = { isSold = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Red,
                            checkedTrackColor = Color.Red.copy(alpha = 0.3f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan / Kondisi Fisik") },
                    placeholder = { Text("Mulus 95%, baret halus pemakaian") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cost = costPriceText.toLongOrNull() ?: 0L
                    val sell = sellingPriceText.toLongOrNull() ?: 0L
                    onSave(
                        brand,
                        clubType,
                        type,
                        loft,
                        isAdjustable,
                        releaseYear,
                        shaft,
                        grip,
                        conditionPercentSlider.toInt().coerceIn(10, 100),
                        cost,
                        sell,
                        isSold,
                        notes
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = GolfGreenPrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Simpan Data", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!isNewItem && onDelete != null) {
                    TextButton(onClick = { showDeleteConfirmDialog = true }) {
                        Text("Hapus", color = Color.Red, fontWeight = FontWeight.SemiBold)
                    }
                }
                OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
                    Text("Back")
                }
            }
        }
    )

    // Fullscreen Image Dialog Preview with Pinch-to-Zoom & Pan (No hint overlay)
    if (showFullScreenImage && !club?.imagePath.isNullOrBlank()) {
        Dialog(onDismissRequest = { showFullScreenImage = false }) {
            var scale by remember { mutableStateOf(1f) }
            var offsetX by remember { mutableStateOf(0f) }
            var offsetY by remember { mutableStateOf(0f) }

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.98f)
                    .fillMaxHeight(0.9f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = rememberAsyncImagePainter(model = club?.imagePath),
                    contentDescription = "Full Screen Zoomable Photo",
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 5f)
                                if (scale > 1f) {
                                    offsetX += pan.x
                                    offsetY += pan.y
                                } else {
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            }
                        }
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY
                        ),
                    contentScale = ContentScale.Fit
                )

                // Close Button
                IconButton(
                    onClick = { showFullScreenImage = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup",
                        tint = Color.White
                    )
                }
            }
        }
    }

    // Brand Picker Search Dialog (Scrolling & Searching master brand list)
    if (showBrandPicker) {
        BrandPickerDialog(
            brandList = brandList,
            onSelectBrand = { selected -> brand = selected },
            onDismiss = { showBrandPicker = false }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Hapus Stik Golf?", fontWeight = FontWeight.Bold) },
            text = { Text("Apakah Anda yakin ingin menghapus stik golf ini dari inventory? Tindakan ini tidak dapat dibatalkan.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Back")
                }
            }
        )
    }
}

@Composable
fun BrandPickerDialog(
    brandList: List<BrandEntity>,
    onSelectBrand: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var brandSearchQuery by remember { mutableStateOf("") }
    val filteredBrands = remember(brandList, brandSearchQuery) {
        if (brandSearchQuery.isBlank()) {
            brandList
        } else {
            brandList.filter { it.name.contains(brandSearchQuery, ignoreCase = true) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pilih Merek Stik Golf", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
            ) {
                OutlinedTextField(
                    value = brandSearchQuery,
                    onValueChange = { brandSearchQuery = it },
                    placeholder = { Text("Cari merek...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (filteredBrands.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Merek tidak ditemukan.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredBrands, key = { it.id }) { brandEntity ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelectBrand(brandEntity.name)
                                        onDismiss()
                                    },
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
                            ) {
                                Text(
                                    text = brandEntity.name,
                                    modifier = Modifier.padding(12.dp),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF0F172A)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
                Text("Back")
            }
        }
    )
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = GolfGreenSecondary,
        letterSpacing = 1.sp
    )
}
