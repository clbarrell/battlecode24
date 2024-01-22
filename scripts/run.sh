#!/bin/bash


# Generate the filename based on the current date and time
output_filename="$(date +'%Y%m%d_%H%M%S').txt"
output_dir="matches/"

# ./gradlew build & wait $!

# Run the command and redirect both stdout and stderr to the output file
./gradlew run -Pmaps=DefaultSmall > "${output_dir}${output_filename}" 2>&1 &

# Wait for the background process to finish
wait $!

# Print the lines of output that are prepended by [server] to the terminal
grep -E '\[server\]' "${output_dir}${output_filename}"

echo "Command output has been saved to: ${output_dir}${output_filename}"
