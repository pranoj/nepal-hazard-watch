package watch.nepalhazard.config;

import java.util.List;
import watch.nepalhazard.dto.Peak;

public class NepalPeaks {

    public static final List<Peak> ALL = List.of(
            new Peak("peak-everest", "Mount Everest", 27.98833, 86.92528, 8849),
            new Peak("peak-kangchenjunga", "Kangchenjunga", 27.70250, 88.14667, 8586),
            new Peak("peak-lhotse", "Lhotse", 27.96170, 86.93330, 8516),
            new Peak("peak-makalu", "Makalu", 27.88972, 87.08889, 8485),
            new Peak("peak-choyu", "Cho Oyu", 28.09417, 86.66083, 8188),
            new Peak("peak-dhaulagiri", "Dhaulagiri I", 28.69611, 83.49528, 8167),
            new Peak("peak-manaslu", "Manaslu", 28.54944, 84.56194, 8163),
            new Peak("peak-annapurna1", "Annapurna I", 28.59611, 83.82028, 8091),
            new Peak("peak-gyachungkang", "Gyachung Kang", 28.09806, 86.74222, 7952),
            new Peak("peak-annapurna2", "Annapurna II", 28.53583, 84.12139, 7937),
            new Peak("peak-himalchuli", "Himalchuli", 28.43417, 84.63750, 7893),
            new Peak("peak-ngadichuli", "Ngadi Chuli", 28.50333, 84.56861, 7871),
            new Peak("peak-nuptse", "Nuptse", 27.96640, 86.89000, 7861),
            new Peak("peak-jannu", "Jannu", 27.68250, 88.03750, 7710),
            new Peak("peak-annapurna3", "Annapurna III", 28.58556, 83.98944, 7555),
            new Peak("peak-annapurna4", "Annapurna IV", 28.53750, 84.08278, 7525));
}
