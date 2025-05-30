package com.lalilu.lmusic.compose.screen.songs

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gigamole.composefadingedges.FadingEdgesGravity
import com.gigamole.composefadingedges.content.FadingEdgesContentType
import com.gigamole.composefadingedges.content.scrollconfig.FadingEdgesScrollConfig
import com.gigamole.composefadingedges.fill.FadingEdgesFillType
import com.gigamole.composefadingedges.verticalFadingEdges
import com.lalilu.common.base.SourceType
import com.lalilu.component.base.smartBarPadding
import com.lalilu.component.base.songs.SongsScreenScrollBar
import com.lalilu.component.base.songs.SongsScreenStickyHeader
import com.lalilu.component.card.SongCard
import com.lalilu.component.extension.ItemRecorder
import com.lalilu.component.extension.rememberLazyListAnimateScroller
import com.lalilu.component.extension.startRecord
import com.lalilu.component.navigation.AppRouter
import com.lalilu.component.state
import com.lalilu.lmedia.entity.FileInfo
import com.lalilu.lmedia.entity.LSong
import com.lalilu.lmedia.entity.Metadata
import com.lalilu.lmedia.extension.GroupIdentity
import com.lalilu.lmusic.LMusicTheme
import com.lalilu.lmusic.viewmodel.SongsEvent
import com.lalilu.lplayer.action.MediaControl
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest

@Composable
internal fun SongsScreenContent(
    recorder: ItemRecorder = ItemRecorder(),
    eventFlow: SharedFlow<SongsEvent> = MutableSharedFlow(),
    keys: () -> Collection<Any> = { emptyList() },
    songs: Map<GroupIdentity, List<LSong>> = emptyMap(),
    isSelecting: () -> Boolean = { false },
    isSelected: (LSong) -> Boolean = { false },
    onSelect: (LSong) -> Unit = {},
    onClickGroup: (GroupIdentity) -> Unit = {}
) {
    val density = LocalDensity.current
    val listState: LazyListState = rememberLazyListState()
    val statusBar = WindowInsets.statusBars
    val favouriteIds = state("favourite_ids", emptyList<String>())
    val scroller = rememberLazyListAnimateScroller(
        listState = listState,
        keys = keys
    )

    LaunchedEffect(Unit) {
        eventFlow.collectLatest { event ->
            when (event) {
                is SongsEvent.ScrollToItem -> {
                    scroller.animateTo(
                        key = event.key,
                        isStickyHeader = { it.contentType == "group" },
                        offset = { item ->
                            // 若是 sticky header，则滚动到顶部
                            if (item.contentType == "group") {
                                return@animateTo -statusBar.getTop(density)
                            }

                            val closestStickyHeaderSize = listState.layoutInfo.visibleItemsInfo
                                .lastOrNull { it.index < item.index && it.contentType == "group" }
                                ?.size ?: 0

                            -(statusBar.getTop(density) + closestStickyHeaderSize)
                        }
                    )
                }

                else -> {}
            }
        }
    }

    SongsScreenScrollBar(
        modifier = Modifier.fillMaxSize(),
        listState = listState
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .verticalFadingEdges(
                    length = statusBar
                        .asPaddingValues()
                        .calculateTopPadding(),
                    contentType = FadingEdgesContentType.Dynamic.Lazy.List(
                        scrollConfig = FadingEdgesScrollConfig.Dynamic(),
                        state = listState
                    ),
                    gravity = FadingEdgesGravity.Start,
                    fillType = remember {
                        FadingEdgesFillType.FadeClip(
                            fillStops = Triple(0f, 0.7f, 1f)
                        )
                    }
                ),
            state = listState,
        ) {
            startRecord(recorder) {
                itemWithRecord(key = "全部歌曲") {
                    val count = remember(songs) { songs.values.flatten().size }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .statusBarsPadding(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "全部歌曲",
                            fontSize = 20.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colors.onBackground
                        )
                        Text(
                            text = "共 $count 首歌曲",
                            color = MaterialTheme.colors.onBackground.copy(0.6f),
                            fontSize = 12.sp,
                            lineHeight = 12.sp,
                        )
                    }
                }

                songs.forEach { (group, list) ->
                    if (group !is GroupIdentity.None) {
                        stickyHeaderWithRecord(
                            key = group,
                            contentType = "group"
                        ) {
                            SongsScreenStickyHeader(
                                listState = listState,
                                group = group,
                                minOffset = { statusBar.getTop(density) },
                                onClickGroup = onClickGroup
                            )
                        }
                    }

                    itemsWithRecord(
                        items = list,
                        key = { it.id },
                        contentType = { it::class.java }
                    ) {
                        SongCard(
                            song = { it },
                            isFavour = { favouriteIds.value.contains(it.id) },
                            isSelected = { isSelected(it) },
                            onClick = {
                                if (isSelecting()) {
                                    onSelect(it)
                                } else {
                                    MediaControl.playWithList(
                                        mediaIds = list.map(LSong::id),
                                        mediaId = it.id
                                    )
                                }
                            },
                            onLongClick = {
                                if (isSelecting()) {
                                    onSelect(it)
                                } else {
                                    AppRouter.route("/pages/songs/detail")
                                        .with("mediaId", it.id)
                                        .jump()
                                }
                            },
                            onEnterSelect = { onSelect(it) }
                        )
                    }
                }
            }

            smartBarPadding()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SongsScreenContentPreview(modifier: Modifier = Modifier) {
    LMusicTheme {
        SongsScreenContent(
            songs = mapOf(
                GroupIdentity.None to emptyList(),
                GroupIdentity.FirstLetter("A") to buildList {
                    repeat(20) { add(testItem(it)) }
                }
            )
        )
    }
}

private fun testItem(id: Int) = LSong(
    id = "$id",
    metadata = Metadata(
        title = "Test",
        album = "album",
        artist = "artist",
        albumArtist = "albumArtist",
        composer = "composer",
        lyricist = "lyricist",
        comment = "comment",
        genre = "genre",
        track = "track",
        disc = "disc",
        date = "date",
        duration = 100000,
        dateAdded = 0,
        dateModified = 0
    ),
    fileInfo = FileInfo(
        mimeType = "audio/mp3",
        directoryPath = "directoryPath",
        pathStr = "pathStr",
        fileName = "fileName",
        size = 1000
    ),
    uri = Uri.EMPTY,
    sourceType = SourceType.Local
)