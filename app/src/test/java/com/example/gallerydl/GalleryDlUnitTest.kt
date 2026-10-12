package com.example.gallerydl

import com.example.gallerydl.data.models.DefaultCliOptions
import com.example.gallerydl.data.models.DownloadJob
import com.example.gallerydl.data.models.ExtractorInfo
import com.example.gallerydl.data.models.JobStatus
import org.junit.Assert.*
import org.junit.Test

class GalleryDlUnitTest {

    @Test
    fun testDownloadJobModel() {
        val job = DownloadJob(
            url = "https://example.com/gallery/1",
            rawArguments = listOf("-d", "/tmp/dl", "-g", "https://example.com/gallery/1"),
            destinationPath = "/tmp/dl",
            status = JobStatus.RUNNING
        )

        assertNotNull(job.id)
        assertEquals("https://example.com/gallery/1", job.url)
        assertEquals(JobStatus.RUNNING, job.status)
        assertFalse(job.isFinished)
        assertEquals(0, job.filesCount)

        val completedJob = job.copy(
            status = JobStatus.COMPLETED,
            exitCode = 0,
            downloadedFiles = listOf("image1.png", "image2.jpg")
        )
        assertTrue(completedJob.isFinished)
        assertEquals(2, completedJob.filesCount)
        assertEquals(0, completedJob.exitCode)
    }

    @Test
    fun testMultiUrlFormatting() {
        val urls = listOf("https://site.com/1", "https://site.com/2", "https://site.com/3")
        val primaryUrl = urls.first()
        val urlDisplay = if (urls.size > 1) "$primaryUrl (+${urls.size - 1} more)" else primaryUrl

        assertEquals("https://site.com/1 (+2 more)", urlDisplay)
    }

    @Test
    fun testJobCancellationTransition() {
        val runningJob = DownloadJob(
            url = "https://example.com/gallery/test",
            rawArguments = listOf("-d", "/tmp/dl", "https://example.com/gallery/test"),
            destinationPath = "/tmp/dl",
            status = JobStatus.RUNNING
        )
        assertFalse(runningJob.isFinished)

        val cancelledJob = runningJob.copy(
            status = JobStatus.CANCELLED,
            exitCode = 130
        )
        assertTrue(cancelledJob.isFinished)
        assertEquals(JobStatus.CANCELLED, cancelledJob.status)
        assertEquals(130, cancelledJob.exitCode)
    }

    @Test
    fun testDefaultCliOptions() {
        val toggles = DefaultCliOptions.getCommonToggles()
        assertTrue("Common options should include URL-only flag", toggles.any { it.flag == "-g" })
        assertTrue("Common options should include JSON dump flag", toggles.any { it.flag == "-j" })
        assertTrue("Common options should include metadata flag", toggles.any { it.flag == "--write-metadata" })
        assertTrue("Common options should include original filename flag", toggles.any { it.flag == "-f /O" })
    }

    @Test
    fun testExtractorInfoModel() {
        val extractor = ExtractorInfo(
            name = "WikimediaExtractor",
            category = "wikimedia",
            subcategory = "image",
            example = "https://commons.wikimedia.org/wiki/File:Example.jpg",
            description = "Downloads Wikimedia media"
        )

        assertEquals("WikimediaExtractor", extractor.name)
        assertEquals("wikimedia", extractor.category)
        assertTrue(extractor.example.startsWith("https://commons.wikimedia.org"))
    }
}
