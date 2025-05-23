package com.lalilu.lmusic.compose.new_screen

import StatusBarLyric.API.StatusBarLyric
import android.annotation.SuppressLint
import android.media.MediaScannerConnection
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.RomUtils
import com.blankj.utilcode.util.ToastUtils
import com.google.accompanist.flowlayout.FlowRow
import com.lalilu.BuildConfig
import com.lalilu.R
import com.lalilu.RemixIcon
import com.lalilu.common.CustomRomUtils
import com.lalilu.component.IconTextButton
import com.lalilu.component.base.NavigatorHeader
import com.lalilu.component.base.screen.ScreenInfo
import com.lalilu.component.base.screen.ScreenInfoFactory
import com.lalilu.component.base.smartBarPadding
import com.lalilu.component.extension.rememberFixedStatusBarHeightDp
import com.lalilu.component.settings.SettingCategory
import com.lalilu.component.settings.SettingFilePicker
import com.lalilu.component.settings.SettingProgressSeekBar
import com.lalilu.component.settings.SettingStateSeekBar
import com.lalilu.component.settings.SettingSwitcher
import com.lalilu.crash.CrashHelper
import com.lalilu.lmedia.scanner.FileSystemScanner
import com.lalilu.lmusic.GuidingActivity
import com.lalilu.lmusic.datastore.SettingsSp
import com.lalilu.lmusic.utils.EQHelper
import com.lalilu.lmusic.utils.extension.getActivity
import com.lalilu.remixicon.System
import com.lalilu.remixicon.system.settings4Line
import com.zhangke.krouter.annotation.Destination
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.math.roundToInt

@Destination("/pages/settings")
object SettingsScreen : Screen, ScreenInfoFactory {
    private fun readResolve(): Any = SettingsScreen

    @Composable
    override fun provideScreenInfo(): ScreenInfo = remember {
        ScreenInfo(
            title = { stringResource(id = R.string.screen_title_settings) },
            icon = RemixIcon.System.settings4Line,
        )
    }

    @Composable
    override fun Content() {
        SettingsScreen()
    }
}


@SuppressLint("PrivateApi")
@Composable
private fun SettingsScreen(
    eqHelper: EQHelper = koinInject(),
    settingsSp: SettingsSp = koinInject(),
    statusBarLyricExt: StatusBarLyric = koinInject(),
    fileSystemScanner: FileSystemScanner = koinInject()
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val darkModeOption = settingsSp.darkModeOption
    val ignoreAudioFocus = settingsSp.ignoreAudioFocus
    val enableUnknownFilter = settingsSp.enableUnknownFilter
    val statusBarLyric = settingsSp.enableStatusLyric
    val lyricGravity = settingsSp.lyricGravity
    val lyricTextSize = settingsSp.lyricTextSize
    val playMode = settingsSp.playMode
    val volumeControl = settingsSp.volumeControl
    val lyricTypefacePath = settingsSp.lyricTypefacePath
    val enableSystemEq = settingsSp.enableSystemEq
    val enableDynamicTips = settingsSp.enableDynamicTips
    val autoHideSeekBar = settingsSp.autoHideSeekbar
    val forceHideStatusBar = settingsSp.forceHideStatusBar
    val keepScreenOnWhenLyricExpanded = settingsSp.keepScreenOnWhenLyricExpanded
    val durationFilter = settingsSp.durationFilter

    val launcherForAudioFx = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
    }

    LazyColumn(
        contentPadding = PaddingValues(top = rememberFixedStatusBarHeightDp())
    ) {
        item {
            NavigatorHeader(
                title = stringResource(id = R.string.screen_title_settings),
                subTitle = stringResource(id = R.string.destination_subtitle_settings)
            )
        }

        item {
            SettingCategory(
                iconRes = R.drawable.ic_settings_4_line,
                titleRes = R.string.preference_player_settings
            ) {
                SettingSwitcher(
                    titleRes = R.string.preference_player_settings_ignore_audio_focus,
                    state = ignoreAudioFocus
                )
                SettingProgressSeekBar(
                    value = { volumeControl.value.toFloat() },
                    onValueUpdate = { volumeControl.value = it.roundToInt() },
                    title = "独立音量控制",
                    valueRange = 0..100
                )
                SettingStateSeekBar(
                    state = playMode,
                    selection = listOf("列表循环", "单曲循环", "随机播放"),
                    title = "播放模式"
                )
                SettingSwitcher(
                    state = enableSystemEq,
                    title = "启用系统均衡器",
                    subTitle = "实验性功能，存在较大机型差异"
                )
                val enableSystemEqValue by enableSystemEq
                AnimatedVisibility(visible = enableSystemEqValue) {
                    Row(
                        Modifier.padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconTextButton(
                            text = "系统均衡器",
                            iconPainter = painterResource(id = R.drawable.equalizer_line),
                            showIcon = { true },
                            color = Color(0xFF006E7C),
                            onClick = {
                                eqHelper.startSystemEqActivity {
                                    launcherForAudioFx.launch(it)
                                }
                            }
                        )
                    }
                }
            }
        }

        item {
            SettingCategory(
                iconRes = com.lalilu.component.R.drawable.ic_lrc_fill,
                titleRes = R.string.preference_lyric_settings
            ) {
                if (RomUtils.isMeizu() || statusBarLyricExt.hasEnable() || CustomRomUtils.isFlyme) {
                    SettingSwitcher(
                        titleRes = R.string.preference_lyric_settings_status_bar_lyric,
                        state = statusBarLyric
                    )
                }
                SettingSwitcher(
                    title = "歌词页展开时隐藏其他组件",
                    subTitle = "简化界面显示效果",
                    state = autoHideSeekBar,
                )
                SettingSwitcher(
                    title = "歌词页展开时屏幕常亮",
                    subTitle = "小心烧屏",
                    state = keepScreenOnWhenLyricExpanded,
                )
                SettingFilePicker(
                    state = lyricTypefacePath,
                    title = "自定义字体",
                    subTitle = "请选择TTF格式的字体文件",
                    mimeType = "font/ttf"
                )
                SettingStateSeekBar(
                    state = lyricGravity,
                    selection = stringArrayResource(id = R.array.lyric_gravity_text).toList(),
                    titleRes = R.string.preference_lyric_settings_text_gravity
                )
//                SettingProgressSeekBar(
//                    state = lyricTextSize,
//                    title = "歌词文字大小",
//                    valueRange = 14..36
//                )
            }
        }

        item {
            SettingCategory(
                iconRes = R.drawable.ic_scan_line,
                titleRes = R.string.preference_media_source_settings
            ) {
//                SettingProgressSeekBar(
//                    state = durationFilter,
//                    title = "筛除小于时长的文件",
//                    valueRange = 0..60
//                )
                SettingSwitcher(
                    state = enableUnknownFilter,
                    titleRes = R.string.preference_media_source_settings_unknown_filter,
                    subTitleRes = R.string.preference_media_source_tips
                )
            }
        }

        item {
            SettingCategory(
                icon = painterResource(id = R.drawable.ic_loader_line),
                title = "其他"
            ) {
                SettingSwitcher(
                    title = "全局隐藏状态栏",
                    subTitle = "简化界面显示效果",
                    state = forceHideStatusBar,
                )
                SettingStateSeekBar(
                    state = darkModeOption,
                    selection = stringArrayResource(id = R.array.dark_mode_options).toList(),
                    titleRes = R.string.preference_dark_mode
                )
                SettingSwitcher(
                    state = enableDynamicTips,
                    titleRes = R.string.preference_media_source_settings_enable_dynamic_tips,
                    subTitleRes = R.string.preference_dynamic_tips
                )
                FlowRow(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    mainAxisSpacing = 10.dp
                ) {
                    IconTextButton(
                        text = "新手引导",
                        color = Color(0xFF3EA22C),
                        onClick = {
                            context.getActivity()?.apply {
                                ActivityUtils.startActivity(GuidingActivity::class.java)
                            }
                        })

                    IconTextButton(
                        text = "日志分享",
                        color = Color(0xFF0040FF),
                        onClick = {
                            scope.launch {
                                context.getActivity()?.apply {
                                    CrashHelper.shareLog(this)
                                } ?: run {
                                    ToastUtils.showShort("日志分享失败")
                                }
                            }
                        }
                    )

                    IconTextButton(
                        text = "MediaStore重新扫描",
                        color = Color(0xFFFF8B3F),
                        onClick = {
                            Toast.makeText(context, "扫描开始", Toast.LENGTH_SHORT).show()
                            // TODO 存在扫描不到的情况，改进方向为先遍历出fileList然后交由其进行scanFile
                            MediaScannerConnection.scanFile(
                                context, arrayOf("/storage/emulated/0/"), null
                            ) { path, uri ->
                                Toast.makeText(context, "扫描结束", Toast.LENGTH_SHORT).show()
                                LogUtils.i("MediaScannerConnection", "path: $path, uri: $uri")
                            }
                        }
                    )

                    IconTextButton(
                        text = "FileSystem重新扫描",
                        color = Color(0xFFFF8B3F),
                        onClick = {
                            Toast.makeText(context, "扫描开始", Toast.LENGTH_SHORT).show()
                            fileSystemScanner.updateAsync()
                        }
                    )
                    IconTextButton(
                        text = "备份数据",
                        color = Color(0xFFFF8B3F),
                        onClick = {
                            ToastUtils.showShort("重做中...")
//                            val json = settingsSp.backup()
//                            clipboardManager.setText(AnnotatedString(json))
                        }
                    )
                    IconTextButton(
                        text = "恢复数据",
                        color = Color(0xFFFF8B3F),
                        onClick = {
                            ToastUtils.showShort("重做中...")
//                            val json = clipboardManager.getText()?.text
//                            json?.let { settingsSp.restore(it) }
                        }
                    )

                    if (BuildConfig.DEBUG) {
                        IconTextButton(
                            text = "测试异常捕获",
                            color = Color(0xFFF12121),
                            onClick = {
                                throw RuntimeException("Exception test")
                            }
                        )
                    }
                }
            }
        }

        smartBarPadding()
    }
}