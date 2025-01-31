package com.leboncointest.android.presentation.viewModel.album

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.leboncointest.android.data.model.dataLocal.AlbumRoom
import com.leboncointest.android.data.model.dataRemote.response.Album
import com.leboncointest.android.domain.usecase.DeleteLocalAlbumUseCase
import com.leboncointest.android.domain.usecase.GetLocalAlbumUseCase
import com.leboncointest.android.domain.usecase.GetRemoteAlbumUseCase
import com.leboncointest.android.domain.usecase.SaveAlbumUseCase
import com.leboncointest.android.presentation.util.isNetworkAvailable
import com.leboncointest.android.ui.UIEvent.Event.AlbumEvent
import com.leboncointest.android.ui.UIEvent.ScreenState.AlbumListScreenState
import com.leboncointest.android.ui.UIEvent.UIEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumViewModel @Inject constructor(
    private val getRemoteAlbumUseCase: GetRemoteAlbumUseCase,
    private val saveAlbumUseCase: SaveAlbumUseCase,
    private val deleteLocalAlbumUseCase: DeleteLocalAlbumUseCase,
    private val getLocalAlbumUseCase: GetLocalAlbumUseCase
) : ViewModel() {

    /**
     * Here we initialize our product screen state
     * This attribute help use to observe in the viewModel
     * The state of our album list ui
     */
    private val _screenStateAlbums = mutableStateOf(
        AlbumListScreenState()
    )

    /**
     * Here we call the State Class to actAlbumListScreenStateivate the State driven in our album list
     */
    val screenStateAlbums: State<AlbumListScreenState> = _screenStateAlbums

    //This Flow help use to displaying our error message with the snack bar ui component
    private val _uiEventFlow = MutableSharedFlow<UIEvent>()
    val uiEventFlow = _uiEventFlow.asSharedFlow()

    // Here we make our http Request to get our album list
     fun getRemoteAlbums() = viewModelScope.launch {
        try {
            //we delete our database cache before reloading the new data if the user is online
            deleteAlbums()
            val apiResult = getRemoteAlbumUseCase.execute()
            apiResult.data?.let { albums ->
                /**
                 * I use the addAll method because
                 * if we have several pages we can
                 * apply Lazy loading infinitely
                 */
                screenStateAlbums.value.albumList.addAll(albums)
                // Here we upgrade our state
                _screenStateAlbums.value = _screenStateAlbums.value.copy(
                    isLoad = false,
                    isNetworkConnected = true
                )

                insertAlbums(albums)
            }
        } catch (e: Exception) {
            _screenStateAlbums.value = _screenStateAlbums.value.copy(
                isNetworkConnected = true,
                isLoad = false,
            )
        }
    }


    //here we insert our all album in our database
    suspend fun insertAlbums(albums: List<Album>) {
        albums.forEach { album ->
            //here we save in our album table
            saveAlbumUseCase.execute(
                album =
                AlbumRoom(
                    id = album.id,
                    albumId = album.albumId,
                    title = album.title,
                    url = album.url,
                    thumbnailUrl = album.thumbnailUrl
                )
            )
        }
    }

    suspend fun deleteAlbums() {
        deleteLocalAlbumUseCase.execute()
    }

    /**
     * We get our album list
     */
    fun getAlbumList() = liveData {
        getLocalAlbumUseCase.execute().collect {
            emit(it)
        }
    }



    //in this method we handle our product list event
    fun onEvent(event: AlbumEvent) {
        when(event) {
            //Here we get call our remote product
            is AlbumEvent.GetRemoteAlbums -> {
                //we activate our progress par spinner
                _screenStateAlbums.value = _screenStateAlbums.value.copy(
                    isLoad = true
                )

                //we check the network state
                if (isNetworkAvailable(event.app)) {
                    //we call our remote product list
                    getRemoteAlbums()
                } else {
                    _screenStateAlbums.value = _screenStateAlbums.value.copy(
                        //we have's  the network connexion
                        isNetworkConnected = false,
                        //if we have's the network error
                        isLoad = false,
                    )
                }
            }

            //we control our network errors
            is AlbumEvent.IsNetworkConnected  -> {
                viewModelScope.launch {
                    _uiEventFlow.emit(
                        UIEvent.ShowMessage(
                            message = event.errorMessage
                        )
                    )
                }
            }
        }
    }



}