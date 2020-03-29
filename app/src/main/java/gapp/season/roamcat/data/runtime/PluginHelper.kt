package gapp.season.roamcat.data.runtime

import android.Manifest
import android.content.Context
import android.content.Intent
import android.view.View
import gapp.season.calculator.CalculatorHelper
import gapp.season.calender.CalenderHelper
import gapp.season.drawboard.DrawBoardHelper
import gapp.season.fileclear.FileClearHelper
import gapp.season.filemanager.FileManager
import gapp.season.fileselector.FileSelectorHelper
import gapp.season.imageviewer.ImageViewerHelper
import gapp.season.manageapps.ManageAppsHelper
import gapp.season.mediastore.MediaStoreHelper
import gapp.season.musicplayer.MusicPlayerHelper
import gapp.season.nerverabbit.NerveRabbitHelper
import gapp.season.notepad.NoteHelper
import gapp.season.poem.PoemReader
import gapp.season.qrcode.QrcodeHelper
import gapp.season.reader.BookReader
import gapp.season.roamcat.R
import gapp.season.roamcat.data.bean.PluginItem
import gapp.season.roamcat.data.event.PluginsUpdateEvent
import gapp.season.roamcat.data.file.MmkvUtil
import gapp.season.roamcat.page.setting.ClipboardActivity
import gapp.season.star.SkyStar
import gapp.season.sudoku.SudokuHelper
import gapp.season.textviewer.TextViewerHelper
import gapp.season.videoplayer.VideoPlayerHelper
import gapp.season.webbrowser.WebViewHelper
import org.greenrobot.eventbus.EventBus

object PluginHelper {
    val allPlugins = mutableListOf<PluginItem>()
    val openPlugins = mutableListOf<PluginItem>()

    fun init(context: Context) {
        //初始化使用插件
        val webPlugin = PluginItem("WebBrowser", PluginItem.TYPE_BASE, R.drawable.icon_browser, context.getString(R.string.item_webbrowser),
                context.getString(R.string.item_msg_webbrowser), View.OnClickListener { WebViewHelper.showWebPage(it.context, null) })
        webPlugin.needPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val readerPlugin = PluginItem("BookReader", PluginItem.TYPE_BASE, R.drawable.icon_book, context.getString(R.string.item_reader),
                context.getString(R.string.item_msg_reader), View.OnClickListener { BookReader.readBook(it.context, null) })
        readerPlugin.needPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val starPlugin = PluginItem("SkyStar", PluginItem.TYPE_EXTEND, R.drawable.icon_moon, context.getString(R.string.item_skystar),
                context.getString(R.string.item_msg_skystar), true, View.OnClickListener { SkyStar.openSkyBall(it.context) })
        val poemPlugin = PluginItem("PoemReader", PluginItem.TYPE_EXTEND, R.drawable.icon_love, context.getString(R.string.item_poem),
                context.getString(R.string.item_msg_poem), true, View.OnClickListener { PoemReader.readPoem(it.context) })
        val appPlugin = PluginItem("Applications", PluginItem.TYPE_BASE, R.drawable.icon_android, context.getString(R.string.item_apps),
                context.getString(R.string.item_msg_apps), View.OnClickListener { ManageAppsHelper.manageApps(it.context) })
        val notePlugin = PluginItem("Note", PluginItem.TYPE_EXTEND, R.drawable.icon_note, context.getString(R.string.item_note),
                context.getString(R.string.item_msg_note), true, View.OnClickListener { NoteHelper.openNote(it.context) })
        notePlugin.needPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val pNotePlugin = PluginItem("PrivateNote", PluginItem.TYPE_EXTEND, R.drawable.icon_privacy, context.getString(R.string.item_private_note),
                context.getString(R.string.item_msg_private_note), true, View.OnClickListener { NoteHelper.openNote(it.context, true) })
        pNotePlugin.needPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        //pNotePlugin.needPermissions.add(Manifest.permission.USE_BIOMETRIC)
        val fmPlugin = PluginItem("FileManager", PluginItem.TYPE_BASE, R.drawable.icon_save, context.getString(R.string.item_file_manager),
                context.getString(R.string.item_msg_file_manager), View.OnClickListener { FileManager.enter(it.context) })
        fmPlugin.needPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val musicPlugin = PluginItem("MusicPlayer", PluginItem.TYPE_BASE, R.drawable.icon_music, context.getString(R.string.item_music),
                context.getString(R.string.item_msg_music), View.OnClickListener { MusicPlayerHelper.play(it.context) })
        musicPlugin.needPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val galleryPlugin = PluginItem("Gallery", PluginItem.TYPE_BASE, R.drawable.icon_album, context.getString(R.string.item_gallery),
                context.getString(R.string.item_msg_gallery), View.OnClickListener { MediaStoreHelper.showGallery(it.context) })
        galleryPlugin.needPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val videoPlugin = PluginItem("Video", PluginItem.TYPE_BASE, R.drawable.icon_video, context.getString(R.string.item_videos),
                context.getString(R.string.item_msg_videos), View.OnClickListener { MediaStoreHelper.showVideos(it.context) })
        videoPlugin.needPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val tvPlugin = PluginItem("TextViewer", PluginItem.TYPE_DEV, R.drawable.icon_layered, context.getString(R.string.item_text_viewer),
                context.getString(R.string.item_msg_text_viewer), View.OnClickListener { TextViewerHelper.show(it.context) })
        val ivPlugin = PluginItem("ImageViewer", PluginItem.TYPE_DEV, R.drawable.icon_personal, context.getString(R.string.item_image_viewer),
                context.getString(R.string.item_msg_image_viewer), View.OnClickListener { ImageViewerHelper.show(it.context) })
        val fcPlugin = PluginItem("FileClear", PluginItem.TYPE_DEV, R.drawable.icon_delete, context.getString(R.string.item_file_clear),
                context.getString(R.string.item_msg_file_clear), true, View.OnClickListener { FileClearHelper.openPage(it.context) })
        fcPlugin.needPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val calePlugin = PluginItem("Calendar", PluginItem.TYPE_EXTEND, R.drawable.icon_schedule, context.getString(R.string.item_calendar),
                context.getString(R.string.item_msg_calendar), View.OnClickListener { CalenderHelper.openCalender(it.context) })
        val calcPlugin = PluginItem("Calculator", PluginItem.TYPE_EXTEND, R.drawable.icon_mlc, context.getString(R.string.item_calculator),
                context.getString(R.string.item_msg_calculator), View.OnClickListener { CalculatorHelper.openCalculator(it.context) })
        val clipPlugin = PluginItem("Clipboard", PluginItem.TYPE_EXTEND, R.drawable.icon_clip, context.getString(R.string.item_clipboard),
                context.getString(R.string.item_msg_clipboard), View.OnClickListener { it.context.startActivity(Intent(it.context, ClipboardActivity::class.java)) })
        val nrPlugin = PluginItem("NerveRabbit", PluginItem.TYPE_EXTEND, R.drawable.icon_child, context.getString(R.string.item_nerve_rabbit),
                context.getString(R.string.item_msg_nerve_rabbit), View.OnClickListener { NerveRabbitHelper.startPlay(it.context) })
        val sudoPlugin = PluginItem("Sudoku", PluginItem.TYPE_EXTEND, R.drawable.icon_mask_clock, context.getString(R.string.item_sudoku),
                context.getString(R.string.item_msg_sudoku), View.OnClickListener { SudokuHelper.openSudoku(it.context) })
        val dbPlugin = PluginItem("DrawingBoard", PluginItem.TYPE_EXTEND, R.drawable.icon_edit, context.getString(R.string.item_drawboard),
                context.getString(R.string.item_msg_drawboard), View.OnClickListener { DrawBoardHelper.openDrawBoard(it.context) })
        dbPlugin.needPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val scanPlugin = PluginItem("ScanQrCode", PluginItem.TYPE_EXTEND, R.drawable.icon_scan_qr, context.getString(R.string.item_scan),
                context.getString(R.string.item_msg_scan), true, View.OnClickListener { QrcodeHelper.scanQrcode(it.context) })
        scanPlugin.needPermissions.add(Manifest.permission.CAMERA)
        scanPlugin.needPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val vpPlugin = PluginItem("VideoPlayer", PluginItem.TYPE_EXTEND, R.drawable.icon_video_play, context.getString(R.string.item_video_player),
                context.getString(R.string.item_msg_video_player), true, View.OnClickListener { VideoPlayerHelper.play(it.context, null) })
        vpPlugin.needPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        vpPlugin.minSysVersion = 19
        val fsPlugin = PluginItem("FileSelector", PluginItem.TYPE_DEV, R.drawable.icon_selection, context.getString(R.string.item_file_selector),
                context.getString(R.string.item_msg_file_selector), View.OnClickListener { FileSelectorHelper.selectFile(it.context, 0, true, null) })
        val musicListPlugin = PluginItem("MusicList", PluginItem.TYPE_EXTEND, R.drawable.icon_musiclist, context.getString(R.string.item_music_list),
                context.getString(R.string.item_msg_music_list), true, View.OnClickListener { MediaStoreHelper.showMusics(it.context) })
        musicListPlugin.needPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        allPlugins.add(appPlugin)
        allPlugins.add(webPlugin)
        allPlugins.add(fmPlugin)
        allPlugins.add(musicPlugin)
        allPlugins.add(galleryPlugin)
        allPlugins.add(videoPlugin)
        allPlugins.add(readerPlugin)
        allPlugins.add(musicListPlugin)
        allPlugins.add(poemPlugin)
        allPlugins.add(notePlugin)
        allPlugins.add(pNotePlugin)
        allPlugins.add(fcPlugin)
        allPlugins.add(starPlugin)
        allPlugins.add(scanPlugin)
        allPlugins.add(vpPlugin)
        allPlugins.add(calePlugin)
        allPlugins.add(calcPlugin)
        allPlugins.add(clipPlugin)
        allPlugins.add(sudoPlugin)
        allPlugins.add(nrPlugin)
        allPlugins.add(dbPlugin)
        allPlugins.add(tvPlugin)
        allPlugins.add(ivPlugin)
        allPlugins.add(fsPlugin)

        updateOpenPlugins()
    }

    fun updateOpenPlugins() {
        openPlugins.clear()
        //取出所有已开启的插件
        allPlugins.forEach {
            if (it.type == PluginItem.TYPE_BASE || (MmkvUtil.map(MmkvUtil.PLUGIN_OPEN_TAG)?.decodeBool(
                            MmkvUtil.PLUGIN_OPEN_TAG + it.name, it.defaultOpen) == true)) {
                it.isOpen = true
                openPlugins.add(it)
            } else {
                it.isOpen = false
            }
        }
        //排序(暂不做排序功能)
        //openPlugins.sortWith(Comparator { o1, o2 -> {} })
        //通知UI更新
        EventBus.getDefault().post(PluginsUpdateEvent())
    }
}
