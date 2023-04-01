package com.f0x1d.sense.ui.screen

import android.app.Activity
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.f0x1d.sense.R
import com.f0x1d.sense.ui.activity.ViewModelFactoryProvider
import com.f0x1d.sense.ui.widget.Message
import com.f0x1d.sense.ui.widget.NavigationBackIcon
import com.f0x1d.sense.ui.widget.TypingMessage
import com.f0x1d.sense.viewmodel.ChatViewModel
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(navController: NavController, chatId: Long) {
    val viewModel = chatViewModel(chatId = chatId)

    val chatWithMessages by viewModel.chatWithMessages.collectAsState(initial = null)

    val text by viewModel.text.observeAsState()
    val addingMyMessage by viewModel.addingMyMessage.observeAsState()

    val lazyListState = rememberLazyListState()

    val scope = rememberCoroutineScope()

    val scrollDownFabVisible by remember {
        derivedStateOf {
            lazyListState.canScrollBackward && addingMyMessage != true && (
                    lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 50
            )
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = chatWithMessages?.chat?.title ?: "",
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )
            },
            navigationIcon = { NavigationBackIcon(navController = navController) }
        )

        Divider()

        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 0.dp,
                    top = 10.dp,
                    end = 0.dp,
                    bottom = 10.dp
                ),
                reverseLayout = true,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                state = lazyListState
            ) {
                val items = chatWithMessages?.messages ?: emptyList()

                itemsIndexed(
                    items,
                    key = { _, it -> it.id }
                ) { index, message ->
                    val needTitle = items.getOrNull(index + 1)?.role != message.role

                    if (message.content == null) {
                        TypingMessage(needTitle = needTitle)
                    } else {
                        Message(
                            message = message,
                            needTitle = needTitle
                        )
                    }
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(5.dp),
                visible = scrollDownFabVisible,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            lazyListState.animateScrollToItem(0)
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_down),
                        contentDescription = null
                    )
                }
            }
        }

        if (chatWithMessages?.messages != null) {
            LaunchedEffect(chatWithMessages!!.messages.size) {
                chatWithMessages?.messages?.also { messages ->
                    lazyListState.animateScrollToItem(0)
                }
                viewModel.addedMyMessage()
            }
        }

        Divider()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .padding(
                        top = 5.dp,
                        start = 10.dp,
                        end = 10.dp,
                        bottom = 10.dp
                    )
                    .weight(1f),
                value = text ?: "",
                onValueChange = { viewModel.updateText(it) },
                label = { Text(text = stringResource(R.string.message)) },
                shape = RoundedCornerShape(20.dp)
            )

            SmallFloatingActionButton(
                modifier = Modifier.padding(end = 10.dp),
                onClick = {
                    viewModel.send(chatWithMessages ?: return@SmallFloatingActionButton)
                },
                elevation = FloatingActionButtonDefaults.elevation(0.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_upward),
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
fun chatViewModel(chatId: Long): ChatViewModel {
    val factory = EntryPointAccessors.fromActivity(
        LocalContext.current as Activity,
        ViewModelFactoryProvider::class.java
    ).chatViewModelFactory()

    return viewModel(factory = ChatViewModel.provideFactory(factory, chatId))
}