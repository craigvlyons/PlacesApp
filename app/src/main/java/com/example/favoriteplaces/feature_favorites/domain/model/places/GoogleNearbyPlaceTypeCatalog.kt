package com.example.favoriteplaces.feature_favorites.domain.model.places

import java.util.Locale

/**
 * Reviewed Google Places Table-A identifiers that are valid in Nearby Search includedTypes.
 *
 * The catalog is deliberately local: Place Autocomplete searches places, not the type vocabulary.
 * Keep this list aligned with Google's Place Types (New) documentation when the Places SDK moves.
 */
object GoogleNearbyPlaceTypeCatalog {
    const val MAX_PICKER_TYPES = 20

    val defaultPickerTypeIds = listOf("restaurant", "coffee_shop", "bar", "bakery")

    private val aliases = mapOf(
        "bar" to setOf("bars", "drinks"),
        "barbecue_restaurant" to setOf("bbq", "barbecue"),
        "breakfast_restaurant" to setOf("breakfast"),
        "brewpub" to setOf("brew pub", "beer"),
        "coffee_shop" to setOf("coffee", "coffeehouse"),
        "fast_food_restaurant" to setOf("fast food"),
        "hamburger_restaurant" to setOf("burger", "burgers"),
        "ice_cream_shop" to setOf("ice cream"),
        "movie_theater" to setOf("cinema", "movies"),
        "pizza_restaurant" to setOf("pizza"),
        "ramen_restaurant" to setOf("ramen"),
        "steak_house" to setOf("steak", "steakhouse"),
        "sushi_restaurant" to setOf("sushi"),
        "taco_restaurant" to setOf("taco", "tacos"),
    )

    val all: List<NearbyCategory> = filterableTypeIds().map { typeId ->
        NearbyCategory(
            storageId = typeId,
            label = friendlyLabel(typeId),
            includedTypes = listOf(typeId),
        )
    }

    private val byId = all.associateBy(NearbyCategory::storageId)

    init {
        check(byId.size == all.size) { "Google Nearby type IDs must be unique." }
        check(defaultPickerTypeIds.all(byId::containsKey)) { "Default picker types must be filterable." }
    }

    fun find(typeId: String?): NearbyCategory? = typeId?.let(byId::get)

    fun require(typeId: String): NearbyCategory = checkNotNull(find(typeId)) {
        "Unknown Google Nearby place type: $typeId"
    }

    fun normalizePickerTypeIds(typeIds: Iterable<String>): List<String> = typeIds
        .map(String::trim)
        .filter(byId::containsKey)
        .distinct()
        .take(MAX_PICKER_TYPES)

    fun search(query: String, limit: Int = 30): List<NearbyCategory> {
        require(limit > 0) { "Search limit must be positive." }
        val normalizedQuery = query.normalizedSearchText()
        if (normalizedQuery.isEmpty()) {
            return SUGGESTED_TYPE_IDS.map(::require).take(limit)
        }
        val queryTokens = normalizedQuery.split(' ').filter(String::isNotEmpty)
        return all.asSequence()
            .mapNotNull { category ->
                val label = category.label.normalizedSearchText()
                val searchable = buildList {
                    add(label)
                    add(category.storageId.replace('_', ' '))
                    addAll(aliases[category.storageId].orEmpty())
                }.map { it.normalizedSearchText() }
                if (queryTokens.all { token -> searchable.any { token in it } }) {
                    category to when {
                        searchable.any { it == normalizedQuery } -> 0
                        label.startsWith(normalizedQuery) -> 1
                        searchable.any { it.startsWith(normalizedQuery) } -> 2
                        else -> 3
                    }
                } else {
                    null
                }
            }
            .sortedWith(compareBy<Pair<NearbyCategory, Int>> { it.second }.thenBy { it.first.label })
            .map(Pair<NearbyCategory, Int>::first)
            .take(limit)
            .toList()
    }

    private fun friendlyLabel(typeId: String): String {
        val words = typeId.split('_').joinToString(" ") { word ->
            when (word) {
                "atm" -> "ATM"
                "ebike" -> "E-bike"
                "rv" -> "RV"
                "us" -> "US"
                else -> word
            }
        }
        return words.replaceFirstChar { character ->
            if (character.isLowerCase()) character.titlecase(Locale.US) else character.toString()
        }
    }

    private fun String.normalizedSearchText(): String = trim()
        .lowercase(Locale.US)
        .replace('_', ' ')
        .splitToSequence(' ', '\t', '\n', '\r')
        .filter(String::isNotEmpty)
        .joinToString(" ")

    private val SUGGESTED_TYPE_IDS = listOf(
        "breakfast_restaurant",
        "brewery",
        "mexican_restaurant",
        "pizza_restaurant",
        "sushi_restaurant",
        "ice_cream_shop",
        "park",
        "hotel",
    )

    private fun filterableTypeIds() = listOf(
        // Automotive
        "car_dealer", "car_rental", "car_repair", "car_wash", "ebike_charging_station",
        "electric_vehicle_charging_station", "gas_station", "parking", "parking_garage",
        "parking_lot", "rest_stop", "tire_shop", "truck_dealer",
        // Business
        "business_center", "corporate_office", "coworking_space", "farm", "manufacturer",
        "ranch", "supplier", "television_studio",
        // Culture
        "art_gallery", "art_museum", "art_studio", "auditorium", "castle",
        "cultural_landmark", "fountain", "historical_place", "history_museum", "monument",
        "museum", "performing_arts_theater", "sculpture",
        // Education
        "academic_department", "educational_institution", "library", "preschool",
        "primary_school", "research_institute", "school", "secondary_school", "university",
        // Entertainment and recreation
        "adventure_sports_center", "amphitheatre", "amusement_center", "amusement_park",
        "aquarium", "banquet_hall", "barbecue_area", "botanical_garden", "bowling_alley",
        "casino", "childrens_camp", "city_park", "comedy_club", "community_center",
        "concert_hall", "convention_center", "cultural_center", "cycling_park", "dance_hall",
        "dog_park", "event_venue", "ferris_wheel", "garden", "go_karting_venue",
        "hiking_area", "historical_landmark", "indoor_playground", "internet_cafe", "karaoke",
        "live_music_venue", "marina", "miniature_golf_course", "movie_rental", "movie_theater",
        "national_park", "night_club", "observation_deck", "off_roading_area", "opera_house",
        "paintball_center", "park", "philharmonic_hall", "picnic_ground", "planetarium",
        "plaza", "roller_coaster", "skateboard_park", "state_park", "tourist_attraction",
        "video_arcade", "vineyard", "visitor_center", "water_park", "wedding_venue",
        "wildlife_park", "wildlife_refuge", "zoo",
        // Facilities and finance
        "public_bath", "public_bathroom", "stable", "accounting", "atm", "bank",
        // Food and drink
        "acai_shop", "afghani_restaurant", "african_restaurant", "american_restaurant",
        "argentinian_restaurant", "asian_fusion_restaurant", "asian_restaurant",
        "australian_restaurant", "austrian_restaurant", "bagel_shop", "bakery",
        "bangladeshi_restaurant", "bar", "bar_and_grill", "barbecue_restaurant",
        "basque_restaurant", "bavarian_restaurant", "beer_garden", "belgian_restaurant",
        "bistro", "brazilian_restaurant", "breakfast_restaurant", "brewery", "brewpub",
        "british_restaurant", "brunch_restaurant", "buffet_restaurant", "burmese_restaurant",
        "burrito_restaurant", "cafe", "cafeteria", "cajun_restaurant", "cake_shop",
        "californian_restaurant", "cambodian_restaurant", "candy_store", "cantonese_restaurant",
        "caribbean_restaurant", "cat_cafe", "chicken_restaurant", "chicken_wings_restaurant",
        "chilean_restaurant", "chinese_noodle_restaurant", "chinese_restaurant",
        "chocolate_factory", "chocolate_shop", "cocktail_bar", "coffee_roastery", "coffee_shop",
        "coffee_stand", "colombian_restaurant", "confectionery", "croatian_restaurant",
        "cuban_restaurant", "czech_restaurant", "danish_restaurant", "deli",
        "dessert_restaurant", "dessert_shop", "dim_sum_restaurant", "diner", "dog_cafe",
        "donut_shop", "dumpling_restaurant", "dutch_restaurant", "eastern_european_restaurant",
        "ethiopian_restaurant", "european_restaurant", "falafel_restaurant", "family_restaurant",
        "fast_food_restaurant", "filipino_restaurant", "fine_dining_restaurant",
        "fish_and_chips_restaurant", "fondue_restaurant", "food_court", "french_restaurant",
        "fusion_restaurant", "gastropub", "german_restaurant", "greek_restaurant",
        "gyro_restaurant", "halal_restaurant", "hamburger_restaurant", "hawaiian_restaurant",
        "hookah_bar", "hot_dog_restaurant", "hot_dog_stand", "hot_pot_restaurant",
        "hungarian_restaurant", "ice_cream_shop", "indian_restaurant", "indonesian_restaurant",
        "irish_pub", "irish_restaurant", "israeli_restaurant", "italian_restaurant",
        "japanese_curry_restaurant", "japanese_izakaya_restaurant", "japanese_restaurant",
        "juice_shop", "kebab_shop", "korean_barbecue_restaurant", "korean_restaurant",
        "latin_american_restaurant", "lebanese_restaurant", "lounge_bar", "malaysian_restaurant",
        "meal_delivery", "meal_takeaway", "mediterranean_restaurant", "mexican_restaurant",
        "middle_eastern_restaurant", "mongolian_barbecue_restaurant", "moroccan_restaurant",
        "noodle_shop", "north_indian_restaurant", "oyster_bar_restaurant", "pakistani_restaurant",
        "pastry_shop", "persian_restaurant", "peruvian_restaurant", "pizza_delivery",
        "pizza_restaurant", "polish_restaurant", "portuguese_restaurant", "pub",
        "ramen_restaurant", "restaurant", "romanian_restaurant", "russian_restaurant",
        "salad_shop", "sandwich_shop", "scandinavian_restaurant", "seafood_restaurant",
        "shawarma_restaurant", "snack_bar", "soul_food_restaurant", "soup_restaurant",
        "south_american_restaurant", "south_indian_restaurant", "southwestern_us_restaurant",
        "spanish_restaurant", "sports_bar", "sri_lankan_restaurant", "steak_house",
        "sushi_restaurant", "swiss_restaurant", "taco_restaurant", "taiwanese_restaurant",
        "tapas_restaurant", "tea_house", "tex_mex_restaurant", "thai_restaurant",
        "tibetan_restaurant", "tonkatsu_restaurant", "turkish_restaurant", "ukrainian_restaurant",
        "vegan_restaurant", "vegetarian_restaurant", "vietnamese_restaurant",
        "western_restaurant", "wine_bar", "winery", "yakiniku_restaurant",
        "yakitori_restaurant",
        // Geographical areas and government
        "administrative_area_level_1", "administrative_area_level_2", "country", "locality",
        "postal_code", "school_district", "city_hall", "courthouse", "embassy", "fire_station",
        "government_office", "local_government_office", "neighborhood_police_station", "police",
        "post_office",
        // Health and wellness
        "chiropractor", "dental_clinic", "dentist", "doctor", "drugstore", "general_hospital",
        "hospital", "massage", "massage_spa", "medical_center", "medical_clinic", "medical_lab",
        "pharmacy", "physiotherapist", "sauna", "skin_care_clinic", "spa", "tanning_studio",
        "wellness_center", "yoga_studio",
        // Housing and lodging
        "apartment_building", "apartment_complex", "condominium_complex", "housing_complex",
        "bed_and_breakfast", "budget_japanese_inn", "campground", "camping_cabin", "cottage",
        "extended_stay_hotel", "farmstay", "guest_house", "hostel", "hotel", "inn",
        "japanese_inn", "lodging", "mobile_home_park", "motel", "private_guest_room",
        "resort_hotel", "rv_park",
        // Natural features and worship
        "beach", "island", "lake", "mountain_peak", "nature_preserve", "river", "scenic_spot",
        "woods", "buddhist_temple", "church", "hindu_temple", "mosque", "shinto_shrine",
        "synagogue",
        // Services
        "aircraft_rental_service", "association_or_organization", "astrologer", "barber_shop",
        "beautician", "beauty_salon", "body_art_service", "catering_service", "cemetery",
        "chauffeur_service", "child_care_agency", "consultant", "courier_service", "electrician",
        "employment_agency", "florist", "food_delivery", "foot_care", "funeral_home",
        "hair_care", "hair_salon", "insurance_agency", "laundry", "lawyer", "locksmith",
        "makeup_artist", "marketing_consultant", "moving_company", "nail_salon",
        "non_profit_organization", "painter", "pet_boarding_service", "pet_care", "plumber",
        "psychic", "real_estate_agency", "roofing_contractor", "service", "shipping_service",
        "storage", "summer_camp_organizer", "tailor", "telecommunications_service_provider",
        "tour_agency", "tourist_information_center", "travel_agency", "veterinary_care",
        // Shopping
        "asian_grocery_store", "auto_parts_store", "bicycle_store", "book_store",
        "building_materials_store", "butcher_shop", "cell_phone_store", "clothing_store",
        "convenience_store", "cosmetics_store", "department_store", "discount_store",
        "discount_supermarket", "electronics_store", "farmers_market", "flea_market",
        "food_store", "furniture_store", "garden_center", "general_store", "gift_shop",
        "grocery_store", "hardware_store", "health_food_store", "home_goods_store",
        "home_improvement_store", "hypermarket", "jewelry_store", "liquor_store", "market",
        "pet_store", "shoe_store", "shopping_mall", "sporting_goods_store", "sportswear_store",
        "store", "supermarket", "tea_store", "thrift_store", "toy_store", "warehouse_store",
        "wholesaler", "womens_clothing_store",
        // Sports
        "arena", "athletic_field", "fishing_charter", "fishing_pier", "fishing_pond",
        "fitness_center", "golf_course", "gym", "ice_skating_rink", "indoor_golf_course",
        "playground", "race_course", "ski_resort", "sports_activity_location", "sports_club",
        "sports_coaching", "sports_complex", "sports_school", "stadium", "swimming_pool",
        "tennis_court",
        // Transportation
        "airport", "airstrip", "bike_sharing_station", "bridge", "bus_station", "bus_stop",
        "ferry_service", "ferry_terminal", "heliport", "international_airport",
        "light_rail_station", "park_and_ride", "subway_station", "taxi_service", "taxi_stand",
        "toll_station", "train_station", "train_ticket_office", "tram_stop", "transit_depot",
        "transit_station", "transit_stop", "transportation_service", "truck_stop",
    )
}
