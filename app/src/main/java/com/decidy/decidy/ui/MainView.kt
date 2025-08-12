package com.decidy.decidy.ui

import android.app.Application
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.decidy.decidy.domain.model.Choice
import com.decidy.decidy.viewmodel.DecidyViewModel
import com.decidy.decidy.viewmodel.DecidyViewModelFactory
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainView() {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val viewModel: DecidyViewModel = viewModel(
        factory = DecidyViewModelFactory(application)
    )
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            HeaderView(
                title = "Decidy",
                subtitle = "Let me decide for you!",
                background = Color(red = 0.58f, green = 0.722f, blue = 0.71f)
            )
        }

        item {
            OutlinedTextField(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(0.9f),
                value = viewModel.choice,
                onValueChange = { viewModel.updateChoice(it) },
                label = { Text("Option") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = {
                        if (viewModel.canAdd) {
                            viewModel.add()
                            viewModel.updateChoice("")
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar("Please add an option first.")
                            }
                            viewModel.showAlert = true
                        }
                    }
                )
            )
        }

        item {
            DecisionWheel(
                state = viewModel.wheelUiState,
                isSpinning = viewModel.isSpinning,
                onSpinEnd = { viewModel.stopSpin(it) },
                onWeightChangeById = { id, w -> viewModel.updateWeightById(id, w) },
                onToggleChoiceById = { id ->
                    val c = viewModel.choices.firstOrNull { it.id == id }
                    viewModel.toggleActiveChoice(c)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .aspectRatio(1f)
            )

        }

        if (viewModel.chosenChoices.isNotEmpty()) {
            item {
                Text(
                    text = "Chosen Options",
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x4D94B8B5)),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            items(viewModel.chosenChoices) { choice ->
                ChoiceItem(choice, onRemove = viewModel::remove)
            }
        }

        if (viewModel.activeChoices.isNotEmpty()) {
            item {
                Text(
                    text = "Active Options",
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x4D94B8B5)),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            items(viewModel.activeChoices) { choice ->
                ChoiceItem(choice, onRemove = viewModel::remove)
            }
        }

        item {
            SnackbarHost(hostState = snackbarHostState)
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                ElevatedButton(
                    onClick = {
                        viewModel.clearPage()
                        viewModel.clearActiveChoice()
                              },
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text("Clear All")
                }

                ElevatedButton(
                    onClick = {
                        viewModel.resetChosen()
                        viewModel.clearActiveChoice()
                              },
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text("Reset Chosen")
                }

                ElevatedButton(
                    onClick = {
                        if (viewModel.canPick && !viewModel.isSpinning) {
                            viewModel.spinWheel()
                        } else if (!viewModel.canPick) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Please add an option first.")
                            }
                            viewModel.showAlert = true
                        }
                    },
                    enabled = !viewModel.isSpinning,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text("Spin Wheel")
                }
            }
        }
    }
    val selected = viewModel.selectedIndex?.let { index ->
        viewModel.activeChoicesBeforeSpin.getOrNull(index)
    }

    if (selected != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { viewModel.selectedIndex = null },
            confirmButton = {
                ElevatedButton(onClick = { viewModel.selectedIndex = null }) {
                    Text("OK")
                }
            },
            title = { Text("${selected.label}!") },
            text = { Text("Decidy chooses: ${selected.label}") }
        )
    }

}


@Composable
fun HeaderView(title: String, subtitle: String, background: Color) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color(
                    red = 0.918f,
                    green = 0.60f,
                    blue = 0.435f,
                    alpha = 0.3f
                )
            )
    )
    {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .background(background)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .wrapContentSize(Alignment.Center)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

@Composable
fun ChoiceItemCard(choice: Choice) {
    ListItem(
        modifier = Modifier,
        headlineContent = {
            Text(
                text = choice.label,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChoiceItem(
    choice: Choice,
    modifier: Modifier = Modifier,
    onRemove: (Choice) -> Unit
) {
    val context = LocalContext.current
    val currentItem by rememberUpdatedState(choice)
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            when(it) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onRemove(currentItem)
                    Toast.makeText(context, "Item deleted", Toast.LENGTH_SHORT).show()
                }
                SwipeToDismissBoxValue.EndToStart -> return@rememberSwipeToDismissBoxState false
                SwipeToDismissBoxValue.Settled -> return@rememberSwipeToDismissBoxState false
            }
            return@rememberSwipeToDismissBoxState false
        },
        positionalThreshold = { it * .25f }
    )
    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        backgroundContent = { DismissBackground(dismissState)},
        content = {
            ChoiceItemCard(choice)
        })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DismissBackground(dismissState: SwipeToDismissBoxState) {
    val color = when (dismissState.dismissDirection) {
        SwipeToDismissBoxValue.StartToEnd -> Color(0xFFFF1744)
        SwipeToDismissBoxValue.EndToStart -> Color.Transparent
        SwipeToDismissBoxValue.Settled -> Color.Transparent
    }


    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(color)
            .padding(12.dp, 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Icon(
            Icons.Default.Delete,
            contentDescription = "delete"
        )
        Spacer(modifier = Modifier)
    }
}