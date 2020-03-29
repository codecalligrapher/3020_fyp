
# react-native-baggage-module

## Getting started

`$ npm install react-native-baggage-module --save`

### Mostly automatic installation

`$ react-native link react-native-baggage-module`

### Manual installation


#### iOS

1. In XCode, in the project navigator, right click `Libraries` ➜ `Add Files to [your project's name]`
2. Go to `node_modules` ➜ `react-native-baggage-module` and add `RNBaggageModule.xcodeproj`
3. In XCode, in the project navigator, select your project. Add `libRNBaggageModule.a` to your project's `Build Phases` ➜ `Link Binary With Libraries`
4. Run your project (`Cmd+R`)<

#### Android

1. Open up `android/app/src/main/java/[...]/MainActivity.java`
  - Add `import com.reactlibrary.RNBaggageModulePackage;` to the imports at the top of the file
  - Add `new RNBaggageModulePackage()` to the list returned by the `getPackages()` method
2. Append the following lines to `android/settings.gradle`:
  	```
  	include ':react-native-baggage-module'
  	project(':react-native-baggage-module').projectDir = new File(rootProject.projectDir, 	'../node_modules/react-native-baggage-module/android')
  	```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
  	```
      compile project(':react-native-baggage-module')
  	```

#### Windows
[Read it! :D](https://github.com/ReactWindows/react-native)

1. In Visual Studio add the `RNBaggageModule.sln` in `node_modules/react-native-baggage-module/windows/RNBaggageModule.sln` folder to their solution, reference from their app.
2. Open up your `MainPage.cs` app
  - Add `using Baggage.Module.RNBaggageModule;` to the usings at the top of the file
  - Add `new RNBaggageModulePackage()` to the `List<IReactPackage>` returned by the `Packages` method


## Usage
```javascript
import RNBaggageModule from 'react-native-baggage-module';

// TODO: What to do with the module?
RNBaggageModule;
```
  