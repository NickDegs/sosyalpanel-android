package com.nickdegs.sosyalpanel.data

// SosyalPanel bulut + SMS auth yapılandırması. iOS Backend (SupabaseConfig.swift) ile birebir.
object Backend {
    // SMS auth servisi (server'da; subdomain proxy gelince çalışır).
    const val AUTH_BASE = "https://sosyalpanel-auth.nickdegs.com"
    // Supabase REST (bulut veri). anon key public — RLS kullanıcıyı korur.
    const val SUPABASE_URL = "https://supabase.realvirtuality.app"
    const val SUPABASE_ANON_KEY =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJyb2xlIjoiYW5vbiIsImlzcyI6InN1cGFiYXNlIiwiaWF0IjoxNzgxNDcyMzExLCJleHAiOjIwOTY4MzIzMTF9.-a3StUgiVBfsxSoWm120o0iAKsaWUEDv_o54VIRfRU8"
}
