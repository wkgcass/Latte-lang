#!/usr/bin/python
# -*- coding: utf-8 -*-

import os
import sys
import time
from subprocess import Popen, PIPE
import zipfile
import platform

GRADLE_CMD = 'gradle'
JAVA_MIN_VERSION = 1.6
GRADLE_MIN_VERSION = 2.14
PACK_ZIP_DIR = 'latte-build/build/distributions/'

def log(str):
    print (str)

def javaVersion():
    try:
        ps = Popen('java -version'.split(' '), stdout=PIPE, stderr=PIPE)
    except:
        return 0
    lines = ps.stderr.readlines()
    if len(lines) == 0:
        return 0
    ver = lines[0].strip()
    starter = 'java version \"'
    if ver[0:len(starter)] == starter:
        ver = ver[len(starter):-1]
        while '_' in ver:
            ver = ver[0:ver.find('_')]
        if '.' in ver and ver.find('.', 2) != -1:
            ver = ver[0:ver.find('.', 2)]
        return float(ver)
    else:
        return 0

def gradleVersion():
    try:
        ps = Popen((GRADLE_CMD + ' --version').split(' '), stdout=PIPE, stderr=PIPE)
    except:
        return 0
    lines = ps.stdout.readlines()
    if len(lines) < 3:
        return 0
    ver = lines[2].strip()
    starter = 'Gradle '
    if ver[0:len(starter)] == starter:
        ver = ver[len(starter):-1]
        if '.' in ver and ver.find('.', 2) != -1:
            ver = ver[0:ver.find('.', 2)]
        return float(ver)
    else:
        return 0

def check():
    # check java version
    jVer = javaVersion()
    if jVer == 0:
        log('Java not found')
        return False
    if jVer < JAVA_MIN_VERSION:
        log('Java version is %s, but %s or higher required' % (str(jVer), str(JAVA_MIN_VERSION)))
        return False

    # check gradle version
    gVer = gradleVersion()
    if gVer == 0:
        log('Gradle not found [' + GRADLE_CMD + ']')
        return False
    if gVer < GRADLE_MIN_VERSION:
        log('Gradle version is %s, but %s or higher required' % (str(gVer), str(GRADLE_MIN_VERSION)))
        return False
    return True

def buildModule(module):
    log('--- Start to build module [%s] ---' % (module))
    res = os.system('cd %s\n%s clean latteBuild' % (module, GRADLE_CMD))
    return res == 0

def build():
    return \
    buildModule('latte-class-recorder') and \
    buildModule('latte-compiler') and \
    buildModule('latte-gradle-plugin') and \
    buildModule('latte-library') and \
    buildModule('latte-build')

def winLink(script):
    # TODO windows link
    return True

def unixLink(script):
    # some file system do not support `ln`
    # so we create a new shell script
    # which redirect all input arguments to dest script
    f = open('latte', 'w')
    f.write('basepath=$(cd `dirname $0`; pwd)\n')
    f.write('$basepath/' + script + ' $*\n')
    f.close()
    # and chmox +x to both the scripts
    return \
    (0 == os.system('chmod +x latte')) and \
    (0 == os.system('chmod +x ' + script))

def scripts():
    # unzip the latte.jar
    zipF = None
    for f in os.listdir(PACK_ZIP_DIR):
        if f[-4:] == '.zip':
            zipF = f
            break
    if not zipF:
        log('distribution zip file not found')
        return False
    theFile = zipfile.ZipFile(PACK_ZIP_DIR + zipF, 'r')
    for filename in theFile.namelist():
        theFile.extract(filename, "./build/")

    # link the scripts
    script = 'build/' + zipF[0:-4] + '/bin/latte'
    system = platform.system()
    log('System: ' + system)

    linkMethod = None
    if system == 'windows':
        linkMethod = winLink
    else:
        linkMethod = unixLink

    if linkMethod(script):
        return True
    else:
        log('Create shortcut script failed')
        return False

def start():
    log('===================================')
    log('          Building Start           ')
    log('===================================')
    startTime = time.time()
    res = check() and build() and scripts()
    endTime = time.time()
    if res:
        log('===================================')
        log('        Building Successful        ')
        log('===================================')
        log('Total time: %.3f secs' % (endTime - startTime))
    else:
        log('===================================')
        log('         Building Failed           ')
        log('===================================')

if __name__ == "__main__":
    if len(sys.argv) > 1:
        GRADLE_CMD = sys.argv[1]
    start()
