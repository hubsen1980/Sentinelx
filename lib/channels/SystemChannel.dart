import 'dart:io';

import 'package:flutter/services.dart';

class SystemChannel {
  static const platform = const MethodChannel('system.channel');

  static final SystemChannel _singleton = new SystemChannel._internal();

  factory SystemChannel() {
    return _singleton;
  }

  SystemChannel._internal();

  Future<Directory> getDataDir() async {
    var path = await platform.invokeMethod<String>("documentPath");
    return Directory(path);
  }
}
