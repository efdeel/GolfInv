package com.example.golfinv.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.golfinv.data.BrandEntity
import com.example.golfinv.ui.theme.GolfGreenPrimary
import com.example.golfinv.utils.ExcelImporterExporter
import kotlinx.coroutines.launch

@Composable
fun BrandSettingsDialog(
    brands: List<BrandEntity>,
    onAddBrand: (String) -> Unit,
    onUpdateBrand: (BrandEntity, String) -> Unit,
    onDeleteBrand: (BrandEntity) -> Unit,
    onImportBrands: (List<BrandEntity>) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var newBrandName by remember { mutableStateOf("") }
    var editingBrand by remember { mutableStateOf<BrandEntity?>(null) }
    var editBrandName by remember { mutableStateOf("") }

    var showImportConfirmDialog by remember { mutableStateOf(false) }

    // SAF Launchers for Brand Export & Import Excel (.xls)
    val exportBrandsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.ms-excel")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val success = ExcelImporterExporter.exportBrandsToExcelXls(context, uri, brands)
                if (success) {
                    Toast.makeText(context, "Berhasil mengekspor ${brands.size} merek ke file Excel (.xls)!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Gagal mengekspor file Excel.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val importBrandsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val imported = ExcelImporterExporter.importBrandsFromExcelXls(context, uri)
                if (imported.isNotEmpty()) {
                    onImportBrands(imported)
                    Toast.makeText(context, "Berhasil mengimpor ${imported.size} merek! Data lama telah dihapus.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Tidak ada data merek yang ditemukan di file Excel.", Toast.LENGTH_SHORT).show()
                }
            }
        }
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
                        imageVector = Icons.AutoMirrored.Filled.Label,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.padding(6.dp).size(20.dp)
                    )
                }
                Text(
                    text = "Kelola Merek Stik Golf (${brands.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
            ) {
                // Export & Import Excel Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            exportBrandsLauncher.launch("Daftar_Merek_Golf_${System.currentTimeMillis()}.xls")
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export Excel", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            showImportConfirmDialog = true
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Import Excel", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Add New Brand Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newBrandName,
                        onValueChange = { newBrandName = it },
                        placeholder = { Text("Tambah Merek Baru...") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Button(
                        onClick = {
                            if (newBrandName.isNotBlank()) {
                                onAddBrand(newBrandName.trim())
                                newBrandName = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GolfGreenPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Tambah")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable List of Brands
                if (brands.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Belum ada merek terdaftar.", color = Color.Gray, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(brands, key = { it.id }) { brand ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = brand.name,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF0F172A),
                                        modifier = Modifier.weight(1f)
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = {
                                                editingBrand = brand
                                                editBrandName = brand.name
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit Merek",
                                                tint = GolfGreenPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { onDeleteBrand(brand) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Hapus Merek",
                                                tint = Color.Red,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
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
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = GolfGreenPrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Selesai")
            }
        }
    )

    // Confirm Import Brands Dialog (Deletes old data first!)
    if (showImportConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showImportConfirmDialog = false },
            title = { Text("⚠️ Import Merek Stik Golf", fontWeight = FontWeight.Bold) },
            text = { Text("Proses import akan MENGHAPUS SELURUH DATA MEREK YANG LAMA (${brands.size} merek) dan menggantinya dengan data baru dari file Excel (.xls).\n\nApakah Anda yakin ingin melanjutkan?") },
            confirmButton = {
                Button(
                    onClick = {
                        showImportConfirmDialog = false
                        importBrandsLauncher.launch(arrayOf("*/*", "application/vnd.ms-excel", "text/csv"))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Ya, Hapus Data Lama & Import")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showImportConfirmDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Batal")
                }
            }
        )
    }

    // Edit Brand Dialog
    if (editingBrand != null) {
        AlertDialog(
            onDismissRequest = { editingBrand = null },
            title = { Text("Edit Merek Stik Golf", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = editBrandName,
                    onValueChange = { editBrandName = it },
                    label = { Text("Nama Merek") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = editingBrand
                        if (target != null && editBrandName.isNotBlank()) {
                            onUpdateBrand(target, editBrandName.trim())
                            editingBrand = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GolfGreenPrimary)
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { editingBrand = null }) {
                    Text("Back")
                }
            }
        )
    }
}
