package com.rkade;

import io.github.libsdl4j.api.haptic.SDL_Haptic;
import io.github.libsdl4j.api.haptic.SDL_HapticEffect;
import io.github.libsdl4j.api.joystick.SDL_Joystick;
import purejavahidapi.HidDevice;

import java.util.logging.Logger;

import static io.github.libsdl4j.api.Sdl.SDL_Init;
import static io.github.libsdl4j.api.SdlSubSystemConst.*;
import static io.github.libsdl4j.api.haptic.SDL_HapticDirectionEncoding.SDL_HAPTIC_CARTESIAN;
import static io.github.libsdl4j.api.haptic.SDL_HapticEffectType.*;
import static io.github.libsdl4j.api.haptic.SdlHaptic.*;
import static io.github.libsdl4j.api.joystick.SdlJoystick.*;

public class Device {
    private static final Logger logger = Logger.getLogger(Device.class.getName());
    private static final int WAIT_AFTER_EFFECT_UPDATE = 5;
    private String name;
    private String hidPath;
    private SDL_Haptic hapticJoystick;
    private int sineEffectId = -1;
    private int springEffectId = -1;
    private int pullLeftEffectId = -1;

    public Device(HidDevice hidDevice, String path) {
        this.name = hidDevice.getHidDeviceInfo().getProductString();
        this.hidPath = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHidPath() {
        return hidPath;
    }

    public void setHidPath(String hidPath) {
        this.hidPath = hidPath;
    }

    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (Exception ex) {
            logger.warning(ex.getMessage());
        }
    }

    private SDL_Haptic getHapticJoystick() {
        if (hapticJoystick == null) {
            int ret = SDL_Init(SDL_INIT_JOYSTICK | SDL_INIT_HAPTIC | SDL_INIT_GAMECONTROLLER);
            if (ret != 0) {
                logger.severe("Could not initialize SDL");
                return null;
            }
            int numJoysticks = SDL_NumJoysticks();
            SDL_Joystick arduinoFfb = null;
            for (int i = 0; i < numJoysticks; i++) {
                SDL_Joystick gameController = SDL_JoystickOpen(i);
                String sdlDevicePath = SDL_JoystickPath(gameController);
                if (hidPath.equals(sdlDevicePath)) {
                    arduinoFfb = gameController;
                    break;
                }
                SDL_JoystickClose(gameController);
            }
            hapticJoystick = SDL_HapticOpenFromJoystick(arduinoFfb);
        }
        return hapticJoystick;
    }

    public boolean doFfbSine() {
        SDL_Haptic ffbDevice = getHapticJoystick();
        if (sineEffectId < 0) {
            SDL_HapticEffect effect = createEffect(SDL_HAPTIC_SINE);
            sineEffectId = SDL_HapticNewEffect(ffbDevice, effect);
            effect.periodic.direction.type = SDL_HAPTIC_CARTESIAN;
            effect.periodic.direction.dir[0] = 1;
            effect.constant.direction.dir[1] = 0; //Y Position
            effect.periodic.period = 100;
            effect.periodic.magnitude = 9000;
            effect.periodic.length = 2000;
            effect.periodic.attackLength = 120;
            effect.periodic.fadeLength = 120;
            SDL_HapticUpdateEffect(ffbDevice, sineEffectId, effect);
            //seems at least 2 milliseconds sleep needed after update
            sleep(WAIT_AFTER_EFFECT_UPDATE);
        }
        return SDL_HapticRunEffect(ffbDevice, sineEffectId, 1) == 0;
    }

    public boolean doFfbSpring() {
        SDL_Haptic ffbDevice = getHapticJoystick();
        if (springEffectId < 0) {
            SDL_HapticEffect effect = createEffect(SDL_HAPTIC_SPRING);
            springEffectId = SDL_HapticNewEffect(ffbDevice, effect);
            effect.condition.direction.type = SDL_HAPTIC_CARTESIAN;
            effect.condition.delay = 0;
            effect.condition.length = 5000;
            effect.condition.direction.dir[0] = 1;
            effect.constant.direction.dir[1] = 1; //Y Position
            effect.condition.leftCoeff[0] = (short) (30000);
            effect.condition.rightCoeff[0] = (short) (30000);
            effect.condition.leftSat[0] = (short) ((30000) * 10);
            effect.condition.rightSat[0] = (short) ((30000) * 10);
            effect.condition.center[0] = 0;
            SDL_HapticUpdateEffect(ffbDevice, springEffectId, effect);
            //seems at least 2 milliseconds sleep needed after update
            sleep(WAIT_AFTER_EFFECT_UPDATE);
        }
        return SDL_HapticRunEffect(ffbDevice, springEffectId, 1) == 0;
    }

    public boolean doFfbPullLeft() {
        SDL_Haptic ffbDevice = getHapticJoystick();
        if (pullLeftEffectId < 0) {
            SDL_HapticEffect effect = createEffect(SDL_HAPTIC_CONSTANT);
            pullLeftEffectId = SDL_HapticNewEffect(ffbDevice, effect);
            effect.constant.direction.type = SDL_HAPTIC_CARTESIAN;
            effect.constant.direction.dir[0] = 1;
            effect.constant.length = 500;
            effect.constant.delay = 0;
            effect.constant.level = 8000;
            SDL_HapticUpdateEffect(ffbDevice, pullLeftEffectId, effect);
            //seems at least 2 milliseconds sleep needed after update
            sleep(WAIT_AFTER_EFFECT_UPDATE);
        }
        return SDL_HapticRunEffect(ffbDevice, pullLeftEffectId, 1) == 0;
    }

    private SDL_HapticEffect createEffect(int type) {
        SDL_HapticEffect effect = new SDL_HapticEffect();
        //cannot set this directly, or it is zeroed out by HapticNewEffect call
        effect.writeField("type", (short) type);
        return effect;
    }
}
