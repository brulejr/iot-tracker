/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2024 Jon Brule <brulejr@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.jrb.labs.common.scheduler

import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import java.time.Duration
import java.util.concurrent.ScheduledFuture

class TaskSchedulerService: ThreadPoolTaskScheduler() {

    val scheduledTasks: MutableMap<String, ScheduledFuture<Any>> = mutableMapOf()

    fun cancelTask(id: String) {
        val result = scheduledTasks[id]?.cancel(true)
        if (result == true) {
            scheduledTasks.remove(id)
        }
    }

    fun scheduleTaskAtFixedRate(id: String, task: RunnableTask, period: Duration) {
        val future = super.scheduleAtFixedRate(task, period)
        scheduledTasks.plus(id to future)
    }

}