package com.platzi.news.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.platzi.core.database.room.dao.MainNewsDao
import com.platzi.core.database.room.dao.SavedNewsDao
import com.platzi.core.database.room.entities.asEntity
import com.platzi.core.database.room.entities.asExternalArticle
import com.platzi.core.database.room.entities.asSavedEntity
import com.platzi.core.database.room.model.asExternalModelArticle
import com.platzi.core.model.Article
import com.platzi.core.network.api.INewsNetworkDataSource
import com.platzi.news.data.repository.paging.MainNewsRemoteMediator
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

class NewsRepository @Inject constructor(
    private val mainNewsDao: MainNewsDao,
    private val mainNewsRemoteMediator: MainNewsRemoteMediator,
    private val savedNewsDao: SavedNewsDao
) : INewsRepository {

    @OptIn(ExperimentalPagingApi::class)
    override fun getMainNewsPagedDao(): Flow<PagingData<Article>> =
        Pager(
            config = PagingConfig(
                pageSize = INewsNetworkDataSource.PAGE_SIZE,
                prefetchDistance = 1,
                initialLoadSize = INewsNetworkDataSource.PAGE_SIZE,
            ),
            pagingSourceFactory = {
                mainNewsDao.getMainNewsPaged()
            },
            remoteMediator = mainNewsRemoteMediator
        ).flow.map { pagingData -> pagingData.map { it.asExternalModelArticle() }}

    override fun updateArticle(article: Article) {
        mainNewsDao.updateArticle(article.isSaved, article.title)
        if(article.isSaved) savedNewsDao.insertArticle(article.asSavedEntity())
        else savedNewsDao.deleteArticle(article.asSavedEntity())
    }

    override fun getSavedNews() =
        savedNewsDao.getSavedNews().map { it.map { entity -> entity.asExternalArticle() } }
}