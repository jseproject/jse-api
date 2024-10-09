# JavaSound Enhancement Project - Application Programming Interface

JSE-API provides a collection of extended API for the JavaSound subsystem of the Java Platform.

## API
All the API are in 2 classes: [Audios](/src/main/java/javasound/enhancement/sampled/Audios.java) and [Midis](/src/main/java/javasound/enhancement/midi/Midis.java).
They're effectively replacements for `javax.sound.sampled.AudioSystem` and `javax.sound.midi.MidiSystem`, with more useful methods implemented by the backing SPI providers.

## SPI
### Sampled
- [AudioResourceReader](/src/main/java/javasound/enhancement/sampled/spi/AudioResourceReader.java) is an interface to load sampled audio from resources (with a `java.lang.ClassLoader` and a pathname string).
- [AudioCompressionWriter](/src/main/java/javasound/enhancement/sampled/spi/AudioCompressionWriter.java) is an interface to write audio files with a `compression level` or `quality` parameter. (range: 0 for lowest, 1 for highest, 32-bit float point)
- [FormatEncodingProvider](/src/main/java/javasound/enhancement/sampled/spi/FormatEncodingProvider.java) is an interface to provide a "unified" or "standard" way to get or check which `javax.sound.sampled.AudioFormat.Encoding` and `javax.sound.sampled.AudioFileFormat.Type` are supported.

### Midi
- [MidiResourceReader](/src/main/java/javasound/enhancement/midi/spi/MidiResourceReader.java) is an interface to load midi audio from resources (with a `java.lang.ClassLoader` and a pathname string).
- [SoundbankResourceReader](/src/main/java/javasound/enhancement/midi/spi/SoundbankResourceReader.java) is an interface to load midi soundbank from resources (with a `java.lang.ClassLoader` and a pathname string).

## Installing
### Maven
```xml
<dependencies>
    <dependency>
        <groupId>io.github.jseproject</groupId>
        <artifactId>jse-api</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```
### Gradle
```groovy
repositories {
    mavenCentral()
}

dependencies {
    implementation 'io.github.jseproject:jse-api:1.0.0'
}
```

## License
[BSD 3-Clause](/LICENSE)
