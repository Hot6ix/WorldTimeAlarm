package com.simples.j.worldtimealarm.fragments

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import androidx.room.Room
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.simples.j.worldtimealarm.ContentSelectorActivity
import com.simples.j.worldtimealarm.R
import com.simples.j.worldtimealarm.databinding.ContentSelectorFragmentBinding
import com.simples.j.worldtimealarm.etc.PatternItem
import com.simples.j.worldtimealarm.etc.RingtoneItem
import com.simples.j.worldtimealarm.etc.SnoozeItem
import com.simples.j.worldtimealarm.models.ContentSelectorViewModel
import com.simples.j.worldtimealarm.support.ContentSelectorAdapter
import com.simples.j.worldtimealarm.utils.AppDatabase
import com.simples.j.worldtimealarm.utils.DatabaseManager
import com.simples.j.worldtimealarm.utils.MediaCursor
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class ContentSelectorFragment : Fragment(), ContentSelectorAdapter.OnItemSelectedListener, CoroutineScope, ContentSelectorAdapter.OnItemMenuSelectedListener {

    private lateinit var viewModel: ContentSelectorViewModel
    private lateinit var contentSelectorAdapter: ContentSelectorAdapter
    private lateinit var recyclerLayoutManager: LinearLayoutManager
    private lateinit var audioManager: AudioManager
    private lateinit var vibrator: Vibrator
    private lateinit var defaultRingtone: RingtoneItem
    private lateinit var binding: ContentSelectorFragmentBinding
    private lateinit var db: AppDatabase

    private val crashlytics = FirebaseCrashlytics.getInstance()

    private var job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + coroutineExceptionHandler

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        throwable.printStackTrace()

        crashlytics.recordException(throwable)
        if(activity?.isFinishing == false) {
            Toast.makeText(context, getString(R.string.error_occurred), Toast.LENGTH_SHORT).show()
        }
    }

    private fun ArrayList<out Any>.contentEquals(list: ArrayList<out Any>?): Boolean {
        if(list == null) return false

        var isSame = this == list

        if(!isSame) {
            if(this.size != list.size) {
                isSame = false
            }
            else {
                isSame = true
                this.forEachIndexed { index, item ->
                    if(list[index] != item) {
                        return false
                    }
                }
            }
        }

        return isSame
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = ContentSelectorFragmentBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        db = Room.databaseBuilder(requireContext(), AppDatabase::class.java, DatabaseManager.DB_NAME)
                .build()
        audioManager = requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager
        vibrator = requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        activity?.run {
            viewModel = ViewModelProvider(this)[ContentSelectorViewModel::class.java]
        }

    }

    override fun onResume() {
        super.onResume()

        job = launch(coroutineContext) {
            context?.let {
                var isSameContent = false
                when(viewModel.action) {
                    ContentSelectorActivity.ACTION_REQUEST_AUDIO -> {
                        val userRingtone = withContext(Dispatchers.IO) {
                            db.ringtoneItemDao().getAll()
                        }
                        val systemRingtone = withContext(Dispatchers.IO) {
                            MediaCursor.getRingtoneList(it)
                        }

                        defaultRingtone =
                                if(systemRingtone.size > 1) systemRingtone[1] // system has some ringtone, set first valid ringtone as default
                                else systemRingtone[0] // system doesn't have any valid ringtone, set no ringtone

                        // TODO: IllegalStateException: Fragment not attached to Activity is still valid at getString(), but most of tries don't throw exception
                        val ringtoneList = ArrayList<RingtoneItem>().apply {
                            add(RingtoneItem(title = it.getString(R.string.my_ringtone), uri = ContentSelectorAdapter.URI_USER_RINGTONE))
                            add(RingtoneItem(title = it.getString(R.string.add_new), uri = ContentSelectorAdapter.URI_ADD_RINGTONE))
                            addAll(userRingtone)
                            add(RingtoneItem(title = it.getString(R.string.system_ringtone), uri = ContentSelectorAdapter.URI_SYSTEM_RINGTONE))
                            addAll(systemRingtone)

                            if(!contains(viewModel.lastSelectedValue)) viewModel.lastSelectedValue = defaultRingtone
                        }

                        if(::contentSelectorAdapter.isInitialized)
                            isSameContent = ringtoneList.contentEquals(contentSelectorAdapter.array)

                        if(!isSameContent)
                            contentSelectorAdapter = ContentSelectorAdapter(it, ringtoneList, viewModel.lastSelectedValue, defaultRingtone)
                    }
                    ContentSelectorActivity.ACTION_REQUEST_VIBRATION -> {
                        val vibrationList = withContext(Dispatchers.IO) {
                            MediaCursor.getVibratorPatterns(it)
                        }

                        if(::contentSelectorAdapter.isInitialized)
                            isSameContent = vibrationList.contentEquals(contentSelectorAdapter.array)

                        if(!isSameContent)
                            contentSelectorAdapter = ContentSelectorAdapter(it, vibrationList, viewModel.lastSelectedValue)
                    }
                    ContentSelectorActivity.ACTION_REQUEST_SNOOZE -> {
                        val snoozeList = withContext(Dispatchers.IO) {
                            MediaCursor.getSnoozeList(it)
                        }

                        if(::contentSelectorAdapter.isInitialized)
                            isSameContent = snoozeList.contentEquals(contentSelectorAdapter.array)

                        if(!isSameContent)
                            contentSelectorAdapter = ContentSelectorAdapter(it, snoozeList, viewModel.lastSelectedValue)
                    }
                }

                if(!isSameContent) {
                    contentSelectorAdapter.setOnItemSelectedListener(this@ContentSelectorFragment)
                    contentSelectorAdapter.setOnItemMenuSelectedListener(this@ContentSelectorFragment)

                    recyclerLayoutManager = LinearLayoutManager(it, LinearLayoutManager.VERTICAL, false)

                    binding.contentRecyclerview.apply {
                        adapter = contentSelectorAdapter
                        layoutManager = recyclerLayoutManager
                        (this.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when {
            requestCode == ContentSelectorActivity.USER_AUDIO_REQUEST_CODE && resultCode == Activity.RESULT_OK -> {
                data?.data?.also {
                    context?.contentResolver?.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    val name = getNameFromUri(it)

                    launch(coroutineContext) {
                        withContext(Dispatchers.IO) {
                            try {
                                db.ringtoneItemDao().insert(RingtoneItem(title = name, uri = it.toString()))

                                viewModel.lastSelectedValue = RingtoneItem(title = getNameFromUri(it), uri = it.toString())
                                play(it.toString())
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

//                        val isInserted = DatabaseCursor(requireContext()).insertUserRingtone(RingtoneItem(title = name, uri = it.toString()))
//                        if(!isInserted) {
//                            Toast.makeText(context, getString(R.string.exist_ringtone), Toast.LENGTH_SHORT).show()
//                        }
//                        else {
//                            viewModel.lastSelectedValue = RingtoneItem(title = getNameFromUri(it), uri = it.toString())
//                            play(it.toString())
//                        }
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()

        launch(coroutineContext) {
            job.cancelAndJoin()
        }

        activity?.run {
            if(!isChangingConfigurations) {
                viewModel.ringtone?.let {
                    if(it.isPlaying) it.stop()
                }
                if(vibrator.hasVibrator()) vibrator.cancel()
            }
        }
    }

    override fun onItemSelected(index: Int, item: Any, action: Int?) {
        when(item) {
            is RingtoneItem -> {
                if(index > 0) {
                    if(item.uri == ContentSelectorAdapter.URI_ADD_RINGTONE) {
                        val uriIntent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "audio/*"
                            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                        }
                        startActivityForResult(uriIntent, ContentSelectorActivity.USER_AUDIO_REQUEST_CODE)
                    }
                    else {
                        // user selected user ringtone
                        viewModel.ringtone.let {
                            if(item.uri != "null") {
                                if(viewModel.lastSelectedValue != item && action != ContentSelectorAdapter.ACTION_NOT_PLAY) {
                                    it?.stop()
                                    play(item.uri)
                                }
                                else if(it == null || !it.isPlaying) {
                                    if(action != ContentSelectorAdapter.ACTION_NOT_PLAY)
                                        play(item.uri)
                                }
                                else {
                                    it.stop()
                                }
                            }
                        }

                        viewModel.lastSelectedValue = item
                    }
                }
                else
                    viewModel.ringtone?.stop()
            }
            is PatternItem -> {
                vibrate(item.array)
                viewModel.lastSelectedValue = item
            }
            is SnoozeItem -> {
                viewModel.lastSelectedValue = item
            }
        }
    }

    override fun onItemMenuSelected(index: Int, menu: MenuItem, type: Int, item: Any) {
        when(type) {
            ContentSelectorAdapter.TYPE_USER_RINGTONE -> {
                val ringtoneItem = item as RingtoneItem
                when(menu.itemId) {
                    R.id.action_remove -> {
                        val tmpRingtone = try {
                            RingtoneManager.getRingtone(context, Uri.parse(ringtoneItem.uri))
                        } catch(e: Exception) {
                            e.printStackTrace()
                            null
                        }

                        viewModel.ringtone?.let {
                            if(tmpRingtone?.getTitle(context) == it.getTitle(context)) {
                                viewModel.ringtone?.stop()
                            }
                        }

                        // update all alarm items that have deleted ringtone
                        item.uri.let { uri ->
                            launch(coroutineExceptionHandler) {
                                db.alarmItemDao().getAll()
                                        .filter { it.ringtone == uri }
                                        .forEach {
                                            val modified = it.apply {
                                                this.ringtone = defaultRingtone.uri
                                            }
                                            db.alarmItemDao().update(modified)
                                        }

                                db.ringtoneItemDao().delete(uri)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun play(uri: String?) {
        launch(coroutineContext) {
            job.join()

            val audioAttrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()

            viewModel.ringtone?.stop()
            try {
                viewModel.ringtone = RingtoneManager.getRingtone(context, Uri.parse(uri))
                viewModel.ringtone?.audioAttributes = audioAttrs
                viewModel.ringtone?.play()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun vibrate(array: LongArray?) {
        launch(coroutineContext) {
            job.join()

            if(array != null) {
                if(Build.VERSION.SDK_INT < 26) {
                    if(array.size > 1) vibrator.vibrate(array, -1)
                    else vibrator.vibrate(array[0])
                }
                else {
                    if(array.size > 1) vibrator.vibrate(VibrationEffect.createWaveform(array, -1))
                    else vibrator.vibrate(VibrationEffect.createOneShot(array[0], VibrationEffect.DEFAULT_AMPLITUDE))
                }
            }
        }
    }

    private fun getNameFromUri(uri: Uri): String {
        if(uri.scheme == "content") {
            val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
            try {
                cursor?.let {
                    it.moveToFirst()
                    var name = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                    val extensionIndex = name.lastIndexOf('.')
                    if(extensionIndex > 0 && extensionIndex < (name.length - 1)) {
                        name = name.substring(0, extensionIndex)
                    }
                    return name
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                cursor?.close()
            }
        }

        return ""
    }

    companion object {
        const val TAG = "ContentSelectorFragment"

        fun newInstance() = ContentSelectorFragment()
    }

}
