package com.leboncointest.android.ui.UIEvent.Event

import android.content.Context

sealed class AlbumEvent {

    data class GetRemoteAlbums(val app: Context): AlbumEvent()

    object GetLocalAlbums: AlbumEvent()

    data class IsNetworkError(val errorMessage: String): AlbumEvent()

    data class IsNetworkConnected(val errorMessage: String): AlbumEvent()
}
