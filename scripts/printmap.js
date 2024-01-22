function normalizeCoordinates(coordinates) {
  const center = coordinates[0];
  const normalizedCoordinates = coordinates
    .slice(1)
    .map((coord) => [coord[0] - center[0], coord[1] - center[1]]);
  let maxX = 0;
  let maxY = 0;

  normalizedCoordinates.forEach((coord) => {
    maxX = Math.max(maxX, Math.abs(coord[0]) + 1);
    maxY = Math.max(maxY, Math.abs(coord[1]) + 1);
  });
  return [normalizedCoordinates, maxX, maxY];
}

function generateAsciiMap(locs, maxX, maxY) {
  const c = locs[0];
  let t = "    ";
  for (let g = Number(c[0]) - maxX; g <= Number(c[0]) + maxX; g++) {
    t += " " + String(g).padStart(2, " ") + " ";
  }

  const map = [t];

  for (let i = Number(c[1]) + maxY; i >= Number(c[1]) - maxY; i--) {
    let row = String(i).padStart(2, " ") + ": ";
    for (let j = Number(c[0]) - maxX; j <= Number(c[0]) + maxX; j++) {
      const cell =
        i === c[1] && j === c[0]
          ? " MM "
          : locs.some((coord) => coord[0] === j && coord[1] === i)
          ? " ## "
          : " -- ";
      row += cell;
    }
    map.push(row);
  }

  return map.join("\n");
}

function printMap(str) {
  const results = str.matchAll(/\[(\d{1,2})\/(\d{1,2})\]/g);

  const locs = [];

  for (result of results) {
    locs.push([Number(result[1]), Number(result[2])]);
  }

  const [normalizedCoordinates, maxX, maxY] = normalizeCoordinates(locs);

  const asciiMap = generateAsciiMap(locs, maxX, maxY);

  console.log("Map:\n" + asciiMap);
}

//------

printMap("[A: #13743@202] [20/24] Enemies (0/8): [20/20]N-dsq:16-d-4, [20/21]N-dsq:9-d-3, [21/20]N-dsq:17-d-4, [21/21]N-dsq:10-d-3, [22/20]N-dsq:20-d-4, [22/21]N-dsq:13-d-3, [22/23]N-dsq:5-d-2, [24/22]N-dsq:20-d-4,")