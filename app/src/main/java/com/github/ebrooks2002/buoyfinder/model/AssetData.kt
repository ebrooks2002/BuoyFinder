/**
 *
 * Define data classes to parse XML Response and assign to data objects.
 * Uses SimpleXML library's annotated classes.
 *
 * @author Ethan Brooks
 */

package com.github.ebrooks2002.buoyfinder.model

import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@Root(name = "message", strict = false)
data class Message(
    @field:Element(name = "id", required = false)
    var id: Long = 0,

    @field:Element(name = "messengerId", required = false)
    var messengerId: String = "",

    @field:Element(name = "messengerName", required = false)
    var messengerName: String = "",

    @field:Element(name = "unixTime", required = false)
    var unixTime: Long = 0,

    @field:Element(name = "messageType", required = false)
    var messageType: String = "",

    @field:Element(name = "latitude", required = false)
    var latitude: Double = 0.0,

    @field:Element(name = "longitude", required = false)
    var longitude: Double = 0.0,

    @field:Element(name = "modelId", required = false)
    var modelId: String = "",

    @field:Element(name = "showCustomMsg", required = false)
    var showCustomMsg: String = "",

    @field:Element(name = "dateTime", required = false)
    var dateTime: String = "",

    @field:Element(name = "batteryState", required = false)
    var batteryState: String = "",

    @field:Element(name = "hidden", required = false)
    var hidden: Int = 0,

    @field:Element(name = "altitude", required = false)
    var altitude: Int = 0,

    @field:Element(name = "messageContent", required = false)
    var messageContent: String = ""
) {
    fun parseDate(): java.util.Date? {
        return if (dateTime.isNotBlank()) {
            try {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US).parse(dateTime)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    val formattedDate: String
        get() {
            val date = parseDate() ?: return "Date not available"
            // Changed pattern from "MMM dd, yyyy" to "MM/dd/yyyy"
            val formatter = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
            formatter.timeZone = TimeZone.getTimeZone("Africa/Accra") // Ghana Time
            return formatter.format(date)
        }

    val formattedTime: String
        get() {
            val date = parseDate() ?: return "Time not available"
            val formatter = SimpleDateFormat("HH:mm", Locale.getDefault()) // Military Time
            formatter.timeZone = TimeZone.getTimeZone("Africa/Accra") // Ghana Time
            return "${formatter.format(date)} GMT"
        }
}

@Root(name = "messages", strict = false)
data class Messages(
    @field:ElementList(inline = true, entry = "message", required = false)
    var list: List<Message>? = null
)

@Root(name = "feed", strict = false)
data class Feed(
    @field:Element(name = "id", required = false)
    var id: String = "",

    @field:Element(name = "name", required = false)
    var name: String = "",

    @field:Element(name = "description", required = false)
    var description: String = "",

    @field:Element(name = "status", required = false)
    var status: String = "",

    @field:Element(name = "usage", required = false)
    var usage: Int = 0,

    @field:Element(name = "daysRange", required = false)
    var daysRange: Int = 0,

    @field:Element(name = "detailedMessageShown", required = false)
    var detailedMessageShown: Boolean = false,

    @field:Element(name = "type", required = false)
    var type: String = ""
)

@Root(name = "feedMessageResponse", strict = false)
data class FeedMessageResponse(
    @field:Element(name = "count", required = false)
    var count: Int = 0,

    @field:Element(name = "feed", required = false)
    var feed: Feed? = null,

    @field:Element(name = "totalCount", required = false)
    var totalCount: Int = 0,

    @field:Element(name = "activityCount", required = false)
    var activityCount: Int = 0,

    @field:Element(name = "messages", required = false)
    var messages: Messages? = null
)

@Root(name = "response", strict = false)
data class AssetData(
    @field:Element(name = "feedMessageResponse", required = false)
    var feedMessageResponse: FeedMessageResponse? = null,

    // Optional error handling just in case
    @field:Element(name = "errors", required = false)
    var errors: ApiErrors? = null
)

@Root(name = "errors", strict = false)
data class ApiErrors(
    @field:Element(name = "error", required = false)
    var error: ApiError? = null
)

@Root(name = "error", strict = false)
data class ApiError(
    @field:Element(name = "code", required = false)
    var code: String = "",
    @field:Element(name = "text", required = false)
    var text: String = "",
    @field:Element(name = "description", required = false)
    var description: String = ""
)
