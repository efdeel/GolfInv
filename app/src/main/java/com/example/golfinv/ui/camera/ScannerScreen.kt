package com.example.golfinv.ui.camera

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GolfCourse
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.golfinv.data.GolfClubEntity
import com.example.golfinv.ui.MainViewModel
import com.example.golfinv.ui.ScannerState
import com.example.golfinv.ui.inventory.EditGolfClubDialog
import com.example.golfinv.ui.theme.GolfAccentGold
import com.example.golfinv.ui.theme.GolfGreenPrimary
import com.example.golfinv.ui.theme.GolfGreenSecondary

@Composable
fun ScannerScreen(
    viewModel: MainViewModel,
    scannerState: ScannerState,
    onNavigateToInventory: () -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PermissionChecker.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> hasCameraPermission = isGranted }
    )

    val brandList by viewModel.brandList.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    if (!hasCameraPermission) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Camera,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = GolfGreenPrimary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Izin Kamera Diperlukan",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Aplikasi membutuhkan akses kamera untuk mengambil foto dan mengidentifikasi head stik golf.",
                    textAlign = TextAlign.Center,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { launcher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = GolfGreenPrimary)
                ) {
                    Text("Berikan Izin Kamera")
                }
            }
        }
        return
    }

    var cameraController by remember { mutableStateOf<CameraController?>(null) }
    var showEditDialogBeforeSave by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!scannerState.isLocked) {
            // Active Live Camera View
            CameraPreviewView(
                onControllerReady = { controller -> cameraController = controller }
            )

            // Yellow Reticle Frame Overlay (Box Kuning)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(260.dp)
                        .border(3.dp, GolfAccentGold, RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black.copy(alpha = 0.65f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Crop,
                                    contentDescription = null,
                                    tint = GolfAccentGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "POSISIKAN HEAD STIK GOLF DI SINI",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text(
                            text = "(Area kotak kuning ini akan di-crop & dianalisa)",
                            color = GolfAccentGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // Bottom Floating Controls: CAPTURE Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = {
                        cameraController?.captureBitmap { bitmap ->
                            if (bitmap != null) {
                                viewModel.captureHeadAndAnalyze(bitmap)
                            }
                        }
                    },
                    modifier = Modifier
                        .height(64.dp)
                        .padding(horizontal = 24.dp),
                    shape = RoundedCornerShape(32.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GolfGreenPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                        tint = GolfAccentGold
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "CAPTURE",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                }
            }

        } else {
            // Frame is CAPTURED: Display OCR Scan Results Card
            val croppedBitmap = scannerState.lockedBitmap

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = GolfGreenPrimary,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = GolfAccentGold,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Hasil Scan OCR Head",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Display Cropped Head Image
                if (croppedBitmap != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Image(
                                bitmap = croppedBitmap.asImageBitmap(),
                                contentDescription = "Cropped Head Image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            Surface(
                                shape = RoundedCornerShape(bottomEnd = 12.dp),
                                color = Color.Black.copy(alpha = 0.7f),
                                modifier = Modifier.align(Alignment.TopStart)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Crop,
                                        contentDescription = null,
                                        tint = GolfAccentGold,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Target Capture Box Kuning",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (scannerState.isAnalyzing) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = GolfGreenPrimary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Menganalisis Teks OCR Head...",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                } else if (scannerState.identifiedClub != null) {
                    val club = scannerState.identifiedClub

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "HASIL IDENTIFIKASI OCR",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GolfGreenSecondary,
                                    letterSpacing = 0.5.sp
                                )

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFE8F5E9)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.GolfCourse,
                                            contentDescription = null,
                                            tint = GolfGreenSecondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "OCR Scan Match",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GolfGreenSecondary
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // OCR Identified Detail Rows
                            DetailItemRow(label = "Brand / Merek", value = club.brand, isHighlight = true)
                            DetailItemRow(label = "Club Type", value = club.clubType, isHighlight = true)
                            DetailItemRow(label = "Model / Tipe", value = club.modelType)
                            DetailItemRow(
                                label = "Loft Degree",
                                value = "${club.loft} ${if (club.isAdjustable) "(Adjustable)" else "(Fixed)"}"
                            )
                            DetailItemRow(label = "Tahun Release", value = club.releaseYear)

                            if (club.rawOcrText.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Teks OCR Mentah: ${club.rawOcrText}",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (scannerState.savedSuccessfully) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = GolfGreenSecondary.copy(alpha = 0.15f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = GolfGreenSecondary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Berhasil Disimpan ke Inventory!",
                                        fontWeight = FontWeight.Bold,
                                        color = GolfGreenPrimary
                                    )
                                    Text(
                                        text = "Kembali ke mode kamera...",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    } else {
                        // Action Buttons: Edit Detail or Simpan Data
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showEditDialogBeforeSave = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Edit Detail")
                            }

                            Button(
                                onClick = {
                                    viewModel.saveIdentifiedClubToInventory()
                                    viewModel.unlockHead()
                                },
                                modifier = Modifier.weight(1.3f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GolfGreenPrimary)
                            ) {
                                Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Simpan Data")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditDialogBeforeSave && scannerState.identifiedClub != null) {
        val identified = scannerState.identifiedClub
        val tempEntity = GolfClubEntity(
            brand = identified.brand,
            clubType = identified.clubType,
            modelType = identified.modelType,
            loft = identified.loft,
            isAdjustable = identified.isAdjustable,
            releaseYear = identified.releaseYear
        )

        EditGolfClubDialog(
            club = tempEntity,
            isNewItem = false,
            brandList = brandList,
            onDismiss = { showEditDialogBeforeSave = false },
            onSave = { brand, clubType, type, loft, adjustable, year, shaft, grip, condition, cost, sell, isSold, notes ->
                viewModel.saveIdentifiedClubToInventory(
                    customBrand = brand,
                    customClubType = clubType,
                    customType = type,
                    customLoft = loft,
                    customIsAdjustable = adjustable,
                    customReleaseYear = year,
                    shaft = shaft,
                    grip = grip,
                    conditionPercent = condition,
                    costPrice = cost,
                    sellingPrice = sell,
                    isSold = isSold,
                    notes = notes
                )
                showEditDialogBeforeSave = false
                viewModel.unlockHead()
            }
        )
    }
}

@Composable
fun DetailItemRow(label: String, value: String, isHighlight: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val dummy = isHighlight
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                color = Color.Gray
            )
            Text(
                text = value,
                fontSize = if (dummy) 16.sp else 14.sp,
                fontWeight = if (dummy) FontWeight.Bold else FontWeight.SemiBold,
                color = if (dummy) GolfGreenPrimary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
