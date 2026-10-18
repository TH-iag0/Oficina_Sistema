// Geometria reutilizada do projeto EXTRA-O_DADOS_ONVIO (sync-chart.js).
export function lineLabelLayout(pointY) {
  return {
    valueY: pointY - 12,
    badgeY: pointY - 32,
    badgeTop: pointY - 44,
    badgeHeight: 16
  };
}
export function axisMax(maximum) {
  if (maximum <= 5) {
    return 5;
  }
  const rawStep = maximum / 5;
  const power = 10 ** Math.floor(Math.log10(rawStep));
  const factor = [1, 2, 2.5, 5, 10].find(value => value * power >= rawStep);
  return Math.ceil(factor * power) * 5;
}
export function smoothPath(points) {
  if (!points.length) {
    return '';
  }
  if (points.length === 1) {
    return `M ${points[0][0]} ${points[0][1]}`;
  }
  const slopes = points.slice(1).map((point, i) => (point[1] - points[i][1]) / (point[0] - points[i][0]));
  const tangents = points.map((_, i) => {
    if (i === 0) {
      return slopes[0];
    }
    if (i === points.length - 1) {
      return slopes.at(-1);
    }
    return slopes[i - 1] * slopes[i] <= 0 ? 0 : 2 / (1 / slopes[i - 1] + 1 / slopes[i]);
  });
  slopes.forEach((slope, i) => {
    if (slope === 0) {
      tangents[i] = 0;
      tangents[i + 1] = 0;
      return;
    }
    const magnitude = Math.hypot(tangents[i] / slope, tangents[i + 1] / slope);
    if (magnitude > 3) {
      tangents[i] *= 3 / magnitude;
      tangents[i + 1] *= 3 / magnitude;
    }
  });
  return points.slice(1).reduce((path, point, i) => {
    const previous = points[i],
      third = (point[0] - previous[0]) / 3;
    return `${path} C ${previous[0] + third} ${previous[1] + tangents[i] * third}, ${point[0] - third} ${point[1] - tangents[i + 1] * third}, ${point[0]} ${point[1]}`;
  }, `M ${points[0][0]} ${points[0][1]}`);
}
