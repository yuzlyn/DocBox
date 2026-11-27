package com.yuzlyn.mdeditor.data.local

import com.yuzlyn.mdeditor.data.NoteModel

fun NoteEntity.toDomain(): NoteModel = NoteModel(
    id = id,
    title = title,
    uriString = uriString,
    content = content,
    notebookTag = notebookTag,
    lastModified = lastModified,
    createdAt = createdAt,
    verticalLayout = verticalLayout,
    writingGuide = writingGuide,
    backgroundPreset = backgroundPreset,
    backgroundInk = backgroundInk,
    noteFont = noteFont,
    sourceFormat = sourceFormat,
    sourceUri = sourceUri
)

fun NoteModel.toEntity(): NoteEntity = NoteEntity(
    id = id,
    title = title,
    uriString = uriString,
    content = content,
    notebookTag = notebookTag,
    lastModified = lastModified,
    createdAt = createdAt,
    verticalLayout = verticalLayout,
    writingGuide = writingGuide,
    backgroundPreset = backgroundPreset,
    backgroundInk = backgroundInk,
    noteFont = noteFont,
    sourceFormat = sourceFormat,
    sourceUri = sourceUri
)
