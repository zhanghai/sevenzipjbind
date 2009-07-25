
macro(PROCESS_SNIPPLET_LINE LINE_VAR)
    STRING(REGEX REPLACE "&" "&amp;" TMP "${${LINE_VAR}}")
#    STRING(REGEX REPLACE " " "&nbsp;" TMP "${TMP}")
#    STRING(REGEX REPLACE "\t" "&nbsp;&nbsp;&nbsp;&nbsp;" TMP "${TMP}")
    STRING(REGEX REPLACE "(\"[^\"]+\")" "<span class=\"java-string\">\\1</span>" TMP "${TMP}")
    STRING(REGEX REPLACE "(int|long|new|void|return|break)" "<span class=\"java-keyword\">\\1</span>" TMP "${TMP}")
    MESSAGE("${TMP}")
    SET(${LINE_VAR} "${TMP}")
endmacro() 

macro(ADJUST_PADDING SNIPPLET_FILE)
    file(STRINGS "${SNIPPLET_FILE}" LINES)
    SET(PADDING -1)
    FOREACH(LINE ${LINES})
        STRING(REPLACE "\t" "    " LINE "${LINE}")
        IF(NOT FIRST_LINE)
            SET(FIRST_LINE "${LINE}")
#            MESSAGE("FIRST_LINE: ${FIRST_LINE}")
            string(LENGTH "${LINE}" PADDING) 
        ELSE()
#            MESSAGE("LINE: ${LINE}")
            SET(I 0)
            SET(C1 "x")
            SET(C2 "x")
            WHILE("${C1}" STREQUAL "${C2}" )
#                MESSAGE("I=${I}")
                STRING(SUBSTRING "${FIRST_LINE}" ${I} 1 C1)
                STRING(SUBSTRING "${LINE}" ${I} 1 C2)
                MATH(EXPR I "${I} + 1")
            ENDWHILE()
            MATH(EXPR I "${I} - 1")
#            MESSAGE("Done I=${I}")
        ENDIF()
    ENDFOREACH()
    FILE(WRITE "${SNIPPLET_FILE}" "")
    FOREACH(LINE ${LINES})
        STRING(REPLACE "\t" "    " LINE "${LINE}")
        string(LENGTH "${LINE}" LENGTH)
        MATH(EXPR LENGTH "${LENGTH} - ${I}") 
        STRING(SUBSTRING "${LINE}" ${I} ${LENGTH} LINE)
        FILE(APPEND "${SNIPPLET_FILE}" "${LINE}\n")
#        MESSAGE("${I}${LINE}")
    ENDFOREACH()
endmacro()


macro(PROCESS_SNIPPLET SNIPPLET_FILE)
    file(STRINGS ${SNIPPLET_FILE} LINES)
    SET(SNIPPLET_NAME "")
    FOREACH(LINE ${LINES})
        IF(LINE MATCHES "/\\* +END_SNIPPLET +\\*/")
            ADJUST_PADDING("${SNIPPLET_OUTPUT_FILENAME}")
            SET(SNIPPLET_NAME "")
        ENDIF()
        IF(NOT SNIPPLET_NAME STREQUAL "")
            PROCESS_SNIPPLET_LINE(LINE)
            FILE(APPEND ${SNIPPLET_OUTPUT_FILENAME} "${LINE}")
            FILE(APPEND ${SNIPPLET_OUTPUT_FILENAME} "\n")
        ENDIF()
        IF(LINE MATCHES "/\\* +BEGIN_SNIPPLET\\([a-zA-Z0-9]+\\) +\\*/")
            STRING(REGEX REPLACE ".*\\(([a-zA-Z0-9]+)\\).*" "\\1" SNIPPLET_NAME ${LINE})
            MESSAGE("NAME: ${SNIPPLET_NAME}")
            SET(SNIPPLET_OUTPUT_FILENAME "web.components/snipplets/${SNIPPLET_NAME}.html")
            FILE(WRITE ${SNIPPLET_OUTPUT_FILENAME} "")
        ENDIF()
    ENDFOREACH()
endmacro()

function(APPEND_FILE OUTPUT_FILE FILE_TO_APPEND)
    FILE(STRINGS "${FILE_TO_APPEND}" LINES)
    FOREACH(LINE ${LINES})
        FILE(APPEND "${OUTPUT_FILE}" "${LINE}\n")
    ENDFOREACH()
endfunction()

macro(PROCESS_HTML FILENAME)
    SET(INPUT_HTML_FILE "web.components/${FILENAME}")
    SET(OUTPUT_HTML_FILE "web/${FILENAME}")

    file(STRINGS "${INPUT_HTML_FILE}" LINES)
    file(WRITE "${OUTPUT_HTML_FILE}" "")
    APPEND_FILE("${OUTPUT_HTML_FILE}" "web.components/header.html")
    FOREACH(LINE ${LINES})
        IF(LINE MATCHES "^[ \t]*##INCLUDE_SNIPPLET\\([a-zA-Z0-9]+\\)[ \t]*$")
            STRING(REGEX REPLACE "^[ \t]*##INCLUDE_SNIPPLET\\(([a-zA-Z0-9]+)\\)[ \t]*$" "\\1" SNIPPLET_NAME ${LINE})
            MESSAGE("Snipplet: ${SNIPPLET_NAME}")
            APPEND_FILE("${OUTPUT_HTML_FILE}" "web.components/snipplet_header.html")
            APPEND_FILE("${OUTPUT_HTML_FILE}" "web.components/snipplets/${SNIPPLET_NAME}.html")
            APPEND_FILE("${OUTPUT_HTML_FILE}" "web.components/snipplet_footer.html")
        ELSE()
            FILE(APPEND "${OUTPUT_HTML_FILE}" "${LINE}\n")
        ENDIF()
    ENDFOREACH()
    APPEND_FILE("${OUTPUT_HTML_FILE}" "web.components/footer.html")
endmacro()

PROCESS_SNIPPLET("../test/JavaTests/src/net/sf/sevenzipjbinding/junit/snipplets/GetNumberOfItemInArchive.java")
PROCESS_HTML("index.html")
PROCESS_HTML("first_steps.html")