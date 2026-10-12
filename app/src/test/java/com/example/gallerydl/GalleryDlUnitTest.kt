package com.example.gallerydl

import com.example.gallerydl.data.models.DefaultCliOptions
import com.example.gallerydl.data.models.DownloadJob
import com.example.gallerydl.data.models.ExtractorInfo
import com.example.gallerydl.data.models.JobStatus
import com.example.gallerydl.util.CliParseException
import com.example.gallerydl.util.CliTokenizer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class GalleryDlUnitTest {

    // --- CliTokenizer Regression Tests ---

    @Test
    fun testTokenizerSimpleFlags() {
        val tokens = CliTokenizer.tokenize("-g -v --no-skip")
        assertEquals(listOf("-g", "-v", "--no-skip"), tokens)
    }

    @Test
    fun testTokenizerFlagsWithValues() {
        val tokens = CliTokenizer.tokenize("-d /storage/emulated/0/Download --abort 5")
        assertEquals(listOf("-d", "/storage/emulated/0/Download", "--abort", "5"), tokens)
    }

    @Test
    fun testTokenizerQuotedFilterExpressions() {
        val tokens = CliTokenizer.tokenize("""--filter "image_width >= 1000 and rating == 's'" https://example.com/gallery""")
        assertEquals(
            listOf("--filter", "image_width >= 1000 and rating == 's'", "https://example.com/gallery"),
            tokens
        )
    }

    @Test
    fun testTokenizerPathsAndTemplatesWithSpaces() {
        val tokens = CliTokenizer.tokenize("""-d "My Photos/Trip 2026" -f "{category} - {id}.{extension}"""")
        assertEquals(
            listOf("-d", "My Photos/Trip 2026", "-f", "{category} - {id}.{extension}"),
            tokens
        )
    }

    @Test
    fun testTokenizerSingleQuotedStrings() {
        val tokens = CliTokenizer.tokenize("""--chapter-filter 'chapter >= 10' --dest '/sdcard/Manga Download'""")
        assertEquals(
            listOf("--chapter-filter", "chapter >= 10", "--dest", "/sdcard/Manga Download"),
            tokens
        )
    }

    @Test
    fun testTokenizerEscapedQuotesAndBackslashes() {
        val tokens = CliTokenizer.tokenize("""--option "escaped \"value\" here" C:\\Images\\Downloads""")
        assertEquals(
            listOf("--option", "escaped \"value\" here", "C:\\Images\\Downloads"),
            tokens
        )
    }

    @Test
    fun testTokenizerEmptyQuotedValues() {
        val tokens = CliTokenizer.tokenize("""--prefix "" --suffix ''""")
        assertEquals(listOf("--prefix", "", "--suffix", ""), tokens)
    }

    @Test
    fun testTokenizerRejectsUnmatchedDoubleQuote() {
        try {
            CliTokenizer.tokenize("""--filter "image_width >= 1000""")
            fail("Expected CliParseException for unmatched double quote")
        } catch (e: CliParseException) {
            assertTrue(e.message!!.contains("Unmatched double quote"))
        }
    }

    @Test
    fun testTokenizerRejectsUnmatchedSingleQuote() {
        try {
            CliTokenizer.tokenize("""--filter 'unmatched single quote""")
            fail("Expected CliParseException for unmatched single quote")
        } catch (e: CliParseException) {
            assertTrue(e.message!!.contains("Unmatched single quote"))
        }
    }

    @Test
    fun testTokenizerRejectsTrailingBackslash() {
        try {
            CliTokenizer.tokenize("--filter text\\")
            // trailing single backslash is invalid escape
            fail("Expected CliParseException for trailing backslash")
        } catch (e: CliParseException) {
            assertTrue(e.message!!.contains("backslash"))
        }
    }

    @Test
    fun testTokenizerMultipleUrls() {
        val input = "https://example.com/1 https://example.com/2 https://example.com/3"
        val tokens = CliTokenizer.tokenize(input)
        assertEquals(
            listOf("https://example.com/1", "https://example.com/2", "https://example.com/3"),
            tokens
        )
    }

    @Test
    fun testTokenizerExecutablePrefixHandling() {
        // Strip prefix when it is exact first token
        val tokensWithPrefix = CliTokenizer.tokenize("gallery-dl --version https://example.com")
        assertEquals(listOf("--version", "https://example.com"), tokensWithPrefix)

        val tokensWindows = CliTokenizer.tokenize("gallery-dl.exe -j https://example.com")
        assertEquals(listOf("-j", "https://example.com"), tokensWindows)

        // Do NOT strip prefix when gallery-dl is an option value or argument
        val tokensAsArg = CliTokenizer.tokenize("--user-agent gallery-dl")
        assertEquals(listOf("--user-agent", "gallery-dl"), tokensAsArg)
    }

    // --- Service & Lifecycle Transitions ---

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
    fun testJobFailureTransition() {
        val job = DownloadJob(
            url = "https://example.com/gallery/err",
            rawArguments = listOf("-d", "/tmp/dl"),
            destinationPath = "/tmp/dl",
            status = JobStatus.RUNNING
        )
        val failedJob = job.copy(
            status = JobStatus.FAILED,
            exitCode = 1
        )
        assertTrue(failedJob.isFinished)
        assertEquals(JobStatus.FAILED, failedJob.status)
        assertEquals(1, failedJob.exitCode)
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
    fun testMultiUrlFormatting() {
        val urls = listOf("https://site.com/1", "https://site.com/2", "https://site.com/3")
        val primaryUrl = urls.first()
        val urlDisplay = if (urls.size > 1) "$primaryUrl (+${urls.size - 1} more)" else primaryUrl

        assertEquals("https://site.com/1 (+2 more)", urlDisplay)
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

    // --- Concurrency Mutex Serialization Test ---

    @Test
    fun testConcurrencyMutexSerialization() = runBlocking {
        val mutex = Mutex()
        val activeCount = AtomicInteger(0)
        val maxOverlap = AtomicInteger(0)
        val completedCount = AtomicInteger(0)

        // Launch two concurrent simulated execution tasks sharing the Mutex
        val job1 = launch {
            mutex.withLock {
                val current = activeCount.incrementAndGet()
                if (current > maxOverlap.get()) maxOverlap.set(current)
                delay(50)
                activeCount.decrementAndGet()
                completedCount.incrementAndGet()
            }
        }

        val job2 = launch {
            mutex.withLock {
                val current = activeCount.incrementAndGet()
                if (current > maxOverlap.get()) maxOverlap.set(current)
                delay(50)
                activeCount.decrementAndGet()
                completedCount.incrementAndGet()
            }
        }

        job1.join()
        job2.join()

        // maxOverlap must be strictly 1, proving mutual exclusion
        assertEquals("Tasks must execute sequentially under Mutex", 1, maxOverlap.get())
        assertEquals("Both tasks must complete successfully", 2, completedCount.get())
    }

    @Test
    fun testExecutionRecoveryAfterFailure() = runBlocking {
        val mutex = Mutex()
        var firstTaskFailed = false
        var secondTaskSucceeded = false

        // Task 1: Fails with exception
        try {
            mutex.withLock {
                firstTaskFailed = true
                throw RuntimeException("Simulated engine failure")
            }
        } catch (e: Exception) {
            // caught
        }

        // Task 2: Verifies mutex is released and executes normally
        mutex.withLock {
            secondTaskSucceeded = true
        }

        assertTrue("First task ran and failed", firstTaskFailed)
        assertTrue("Second task succeeded after first failed (lock was released)", secondTaskSucceeded)
    }
}
