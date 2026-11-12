package com.example.golfinv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.GolfCourse
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.golfinv.data.GolfDatabase
import com.example.golfinv.data.GolfRepository
import com.example.golfinv.ui.MainViewModel
import com.example.golfinv.ui.MainViewModelFactory
import com.example.golfinv.ui.camera.ScannerScreen
import com.example.golfinv.ui.inventory.InventoryScreen
import com.example.golfinv.ui.theme.GolfAccentGold
import com.example.golfinv.ui.theme.GolfGreenPrimary
import com.example.golfinv.ui.theme.GolfInvTheme

sealed class NavTab(val route: String, val title: String, val icon: ImageVector) {
    object Inventory : NavTab("inventory", "List Inventory", Icons.Default.GolfCourse)
    object Scanner : NavTab("scanner", "Scanner Head", Icons.Default.Camera)
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        val database = GolfDatabase.getDatabase(applicationContext)
        val repository = GolfRepository(database.golfClubDao(), database.brandDao(), applicationContext)
        MainViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GolfInvTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: MainViewModel) {
    // First screen is List Inventory as requested
    var selectedTab by remember { mutableStateOf<NavTab>(NavTab.Inventory) }

    val scannerState by viewModel.scannerState.collectAsStateWithLifecycle()
    val inventoryList by viewModel.inventoryList.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedInventoryTab by viewModel.selectedInventoryTab.collectAsStateWithLifecycle()
    val editingClub by viewModel.editingClub.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = GolfGreenPrimary,
                contentColor = Color.White
            ) {
                val tabs = listOf(NavTab.Inventory, NavTab.Scanner)
                tabs.forEach { tab ->
                    val isSelected = selectedTab.route == tab.route
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GolfAccentGold,
                            unselectedIconColor = Color.White.copy(alpha = 0.6f),
                            selectedTextColor = GolfAccentGold,
                            unselectedTextColor = Color.White.copy(alpha = 0.6f),
                            indicatorColor = Color.Black.copy(alpha = 0.2f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                is NavTab.Inventory -> {
                    InventoryScreen(
                        viewModel = viewModel,
                        clubs = inventoryList,
                        searchQuery = searchQuery,
                        selectedTab = selectedInventoryTab,
                        editingClub = editingClub,
                        onNavigateToScanner = { selectedTab = NavTab.Scanner }
                    )
                }
                is NavTab.Scanner -> {
                    ScannerScreen(
                        viewModel = viewModel,
                        scannerState = scannerState,
                        onNavigateToInventory = { selectedTab = NavTab.Inventory }
                    )
                }
            }
        }
    }
}
