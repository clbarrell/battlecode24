import subprocess
import datetime
import os
import random

# players
team_a = "duck7"
team_b = "duck6"

# Set the directory where the output file will be saved
output_dir = "../matches/"
maps_dir = "../maps/"

# Step 1: Randomly select 3 files from /maps
# https://github.com/battlecode/battlecode24/tree/master/engine/src/main/battlecode/world/resources
MAPS = [
    "AceOfSpades.map24",
    "Alien.map24",
    "Ambush.map24",
    "Battlecode24.map24",
    "BigDucksBigPond.map24",
    "CH3353C4K3F4CT0RY.map24",
    "Canals.map24",
    "DefaultHuge.map24",
    "DefaultLarge.map24",
    "DefaultMedium.map24",
    "DefaultSmall.map24",
    "Duck.map24",
    "Fountain.map24",
    "Hockey.map24",
    "HungerGames.map24",
    "MazeRunner.map24",
    "Rivers.map24",
    "Snake.map24",
    "Soccer.map24",
    "SteamboatMickey.map24",
    "Yinyang.map24",
]
mapsToRun = random.sample(MAPS, 3)


# Step 2: Generate the filename based on the current date and time
output_filename = f"{team_a}-vs-{team_b}-on-{','.join(mapsToRun)}-{datetime.datetime.now().strftime('%Y%m%d_%H%M%S')}.txt"

# ./gradlew run -Pmaps=[map] -PteamA=[Team A] -PteamB=[Team B]
# Run the command with mapsToRun and save the output to the specified file
command = (
    f"./gradlew run -Pmaps={','.join(mapsToRun)} -PteamA={team_a} -PteamB={team_b}"
)
output_file_path = os.path.join(output_dir, output_filename)
with open(output_file_path, "w") as output_file:
    process = subprocess.run(
        command, shell=True, stdout=output_file, stderr=subprocess.STDOUT, cwd="../"
    )

# Step 3: Append (or create) a summary.txt which only lists the server lines
summary_file_path = os.path.join(output_dir, "summary.txt")
with open(output_file_path, "r") as output_file, open(
    summary_file_path, "a"
) as summary_file:
    server_lines = [
        line.strip() for line in output_file.readlines() if line.startswith("[server]")
    ]
    summary_file.write("\n".join(server_lines) + "\n")

# Print the lines of output that are prepended by [server] to the terminal
print("\n".join(server_lines))
print(f"Command output has been saved to: {output_file_path}")
print(f"Server lines summary has been appended to: {summary_file_path}")
