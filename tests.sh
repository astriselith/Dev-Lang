OUTFILE="tests.txt"
INFILE="tests"

find "$INFILE" -type f -exec sh -c '
	for file do
		printf "// ./%s\n// auto generated\n" "$file"
		cat "$file"
		printf "\n"
	done
' sh {} + > "$OUTFILE"