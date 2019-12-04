/*
 * JOCL - Java bindings for OpenCL
 *
 * Copyright (c) 2009-2012 Marco Hutter - http://www.jocl.org
 * 
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package org.jocl;


import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Size constants for scalar and vector data types.
 */
public final class Sizeof 
{

    /**
     * The logger used in this class
     */
    private static final Logger logger =
        Logger.getLogger(Sizeof.class.getName());

    
    /**
     * Size of a cl_char, in bytes.
     */
    public static final int cl_char = 8 / 8;

    /**
     * Size of a cl_uchar, in bytes.
     */
    public static final int cl_uchar = 8 / 8;

    /**
     * Size of a cl_short, in bytes.
     */
    public static final int cl_short = 16 / 8;

    /**
     * Size of a cl_ushort, in bytes.
     */
    public static final int cl_ushort = 16 / 8;

    /**
     * Size of a cl_int, in bytes.
     */
    public static final int cl_int = 32 / 8;

    /**
     * Size of a cl_uint, in bytes.
     */
    public static final int cl_uint = 32 / 8;

    /**
     * Size of a cl_long, in bytes.
     */
    public static final int cl_long = 64 / 8;

    /**
     * Size of a cl_ulong, in bytes.
     */
    public static final int cl_ulong = 64 / 8;

    /**
     * Size of a cl_half, in bytes.
     */
    public static final int cl_half = 16 / 8;

    /**
     * Size of a cl_float, in bytes.
     */
    public static final int cl_float = 32 / 8;

    /**
     * Size of a cl_double, in bytes.
     */
    public static final int cl_double = 64 / 8;

    /**
     * Size of a cl_char2, in bytes
     */
    public static final int cl_char2 = 8 * 2 / 8;

    /**
     * Size of a cl_char4, in bytes
     */
    public static final int cl_char4 = 8 * 4 / 8;
    
    /**
     * Size of a cl_char8, in bytes
     */
    public static final int cl_char8 = 8 * 8 / 8;

    /**
     * Size of a cl_char16, in bytes
     */
    public static final int cl_char16 = 8 * 16 / 8;

    /**
     * Size of a cl_char3, in bytes.<br>
     * <br>
     * This is the same as the size of a cl_char4, according to the OpenCL
     * specification, version 1.1, section 6.1.5
     */
    public static final int cl_char3 = cl_char4;
    
    /**
     * Size of a cl_uchar2, in bytes
     */
    public static final int cl_uchar2 = 8 * 2 / 8;

    /**
     * Size of a cl_uchar4, in bytes
     */
    public static final int cl_uchar4 = 8 * 4 / 8;

    /**
     * Size of a cl_uchar8, in bytes
     */
    public static final int cl_uchar8 = 8 * 8 / 8;

    /**
     * Size of a cl_uchar16, in bytes
     */
    public static final int cl_uchar16 = 8 * 16 / 8;

    /**
     * Size of a cl_uchar3, in bytes.<br>
     * <br>
     * This is the same as the size of a cl_uchar4, according to the OpenCL
     * specification, version 1.1, section 6.1.5
     */
    public static final int cl_uchar3 = cl_uchar4;
    
    /**
     * Size of a cl_short2, in bytes
     */
    public static final int cl_short2 = 16 * 2 / 8;

    /**
     * Size of a cl_short4, in bytes
     */
    public static final int cl_short4 = 16 * 4 / 8;

    /**
     * Size of a cl_short8, in bytes
     */
    public static final int cl_short8 = 16 * 8 / 8;

    /**
     * Size of a cl_short16, in bytes
     */
    public static final int cl_short16 = 16 * 16 / 8;

    /**
     * Size of a cl_short3, in bytes.<br>
     * <br>
     * This is the same as the size of a cl_short4, according to the OpenCL
     * specification, version 1.1, section 6.1.5
     */
    public static final int cl_short3 = cl_short4;
    
    /**
     * Size of a cl_ushort2, in bytes
     */
    public static final int cl_ushort2 = 16 * 2 / 8;

    /**
     * Size of a cl_ushort4, in bytes
     */
    public static final int cl_ushort4 = 16 * 4 / 8;

    /**
     * Size of a cl_ushort8, in bytes
     */
    public static final int cl_ushort8 = 16 * 8 / 8;

    /**
     * Size of a cl_ushort16, in bytes
     */
    public static final int cl_ushort16 = 16 * 16 / 8;

    /**
     * Size of a cl_ushort3, in bytes.<br>
     * <br>
     * This is the same as the size of a cl_ushort4, according to the OpenCL
     * specification, version 1.1, section 6.1.5
     */
    public static final int cl_ushort3 = cl_ushort4;
    
    /**
     * Size of a cl_int2, in bytes
     */
    public static final int cl_int2 = 32 * 2 / 8;

    /**
     * Size of a cl_int4, in bytes
     */
    public static final int cl_int4 = 32 * 4 / 8;

    /**
     * Size of a cl_int8, in bytes
     */
    public static final int cl_int8 = 32 * 8 / 8;

    /**
     * Size of a cl_int16, in bytes
     */
    public static final int cl_int16 = 32 * 16 / 8;

    /**
     * Size of a cl_int3, in bytes.<br>
     * <br>
     * This is the same as the size of a cl_int4, according to the OpenCL
     * specification, version 1.1, section 6.1.5
     */
    public static final int cl_int3 = cl_int4;
    
    /**
     * Size of a cl_uint2, in bytes
     */
    public static final int cl_uint2 = 32 * 2 / 8;

    /**
     * Size of a cl_uint4, in bytes
     */
    public static final int cl_uint4 = 32 * 4 / 8;

    /**
     * Size of a cl_uint8, in bytes
     */
    public static final int cl_uint8 = 32 * 8 / 8;

    /**
     * Size of a cl_uint16, in bytes
     */
    public static final int cl_uint16 = 32 * 16 / 8;

    /**
     * Size of a cl_uint3, in bytes.<br>
     * <br>
     * This is the same as the size of a cl_uint4, according to the OpenCL
     * specification, version 1.1, section 6.1.5
     */
    public static final int cl_uint3 = cl_uint4;
    
    /**
     * Size of a cl_long2, in bytes
     */
    public static final int cl_long2 = 64 * 2 / 8;

    /**
     * Size of a cl_long4, in bytes
     */
    public static final int cl_long4 = 64 * 4 / 8;

    /**
     * Size of a cl_long8, in bytes
     */
    public static final int cl_long8 = 64 * 8 / 8;

    /**
     * Size of a cl_long16, in bytes
     */
    public static final int cl_long16 = 64 * 16 / 8;

    /**
     * Size of a cl_long3, in bytes.<br>
     * <br>
     * This is the same as the size of a cl_long4, according to the OpenCL
     * specification, version 1.1, section 6.1.5
     */
    public static final int cl_long3 = cl_long4;
    
    /**
     * Size of a cl_ulong2, in bytes
     */
    public static final int cl_ulong2 = 64 * 2 / 8;

    /**
     * Size of a cl_ulong4, in bytes
     */
    public static final int cl_ulong4 = 64 * 4 / 8;

    /**
     * Size of a cl_ulong8, in bytes
     */
    public static final int cl_ulong8 = 64 * 8 / 8;

    /**
     * Size of a cl_ulong16, in bytes
     */
    public static final int cl_ulong16 = 64 * 16 / 8;

    /**
     * Size of a cl_ulong3, in bytes.<br>
     * <br>
     * This is the same as the size of a cl_ulong4, according to the OpenCL
     * specification, version 1.1, section 6.1.5
     */
    public static final int cl_ulong3 = cl_ulong4;
    
    /**
     * Size of a cl_float2, in bytes
     */
    public static final int cl_float2 = 32 * 2 / 8;

    /**
     * Size of a cl_float4, in bytes
     */
    public static final int cl_float4 = 32 * 4 / 8;

    /**
     * Size of a cl_float8, in bytes
     */
    public static final int cl_float8 = 32 * 8 / 8;

    /**
     * Size of a cl_float16, in bytes
     */
    public static final int cl_float16 = 32 * 16 / 8;

    /**
     * Size of a cl_float3, in bytes.<br>
     * <br>
     * This is the same as the size of a cl_float4, according to the OpenCL
     * specification, version 1.1, section 6.1.5
     */
    public static final int cl_float3 = cl_float4;
    
    /**
     * Size of a cl_double2, in bytes
     */
    public static final int cl_double2 = 64 * 2 / 8;

    /**
     * Size of a cl_double4, in bytes
     */
    public static final int cl_double4 = 64 * 4 / 8;

    /**
     * Size of a cl_double8, in bytes
     */
    public static final int cl_double8 = 64 * 8 / 8;

    /**
     * Size of a cl_double16, in bytes
     */
    public static final int cl_double16 = 64 * 16 / 8;

    /**
     * Size of a cl_double3, in bytes.<br>
     * <br>
     * This is the same as the size of a cl_double4, according to the OpenCL
     * specification, version 1.1, section 6.1.5
     */
    public static final int cl_double3 = cl_double4;
    
    /**
     * Size of a native pointer, in bytes
     */
    public static final int POINTER = computePointerSize();

    /**
     * Size of a size_t, in bytes
     */
    // TODO: This is assumed to be the same as the pointer size,
    // although this is not clearly specified in the C standard.
    public static final int cl_size_t = POINTER;
    
    /**
     * Size of a cl_platform_id, in bytes
     */
    public static final int cl_platform_id = POINTER;
    
    /**
     * Size of a cl_device_id, in bytes
     */
    public static final int cl_device_id = POINTER;
    
    /**
     * Size of a cl_context, in bytes
     */
    public static final int cl_context = POINTER;
    
    /**
     * Size of a cl_command_queue, in bytes
     */
    public static final int cl_command_queue = POINTER;
    
    /**
     * Size of a cl_mem, in bytes
     */
    public static final int cl_mem = POINTER;
    
    /**
     * Size of a cl_program, in bytes
     */
    public static final int cl_program = POINTER;
    
    /**
     * Size of a cl_kernel, in bytes
     */
    public static final int cl_kernel = POINTER;
    
    /**
     * Size of a cl_event, in bytes
     */
    public static final int cl_event = POINTER;
    
    /**
     * Size of a cl_sampler, in bytes
     */
    public static final int cl_sampler = POINTER;


    /**
     * Computes the size of a pointer, in bytes
     * 
     * @return The size of a pointer, in bytes
     */
    private static int computePointerSize()
    {
        // Android does not have the "sun.arch.data.model" system property, 
        // so the pointer size is computed with a native method
        if (LibUtils.calculateOS() == LibUtils.OSType.ANDROID) 
        {
            return 8;//computePointerSizeNative();
        } 
        
        // Desktop systems may fail to load the native library for the
        // initialization of the (constant) fields in this class, so
        // the pointer size is determined without native methods here.
        // See https://github.com/gpu/JOCL/issues/5
        String bits = System.getProperty("sun.arch.data.model");
        if ("32".equals(bits))
        {
            return 4;
        }
        if ("64".equals(bits))
        {
            return 8;
        }
        logger.log(Level.SEVERE, 
            "Unknown value for sun.arch.data.model - assuming 32 bits");
        return 4;
    }
    private static native int computePointerSizeNative();
}
