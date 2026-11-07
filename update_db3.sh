#!/bin/bash
sed -i "/db?.execSQL(\"INSERT INTO commands (pattern, action, parameters) VALUES ('fake call', 'FAKE_CALL', '')\")/a \        db?.execSQL(\"INSERT INTO commands (pattern, action, parameters) VALUES ('kholo', 'DEEP_LINK', 'search')\")" app/src/main/java/com/kyro/brain/RawBrainQuery.kt
