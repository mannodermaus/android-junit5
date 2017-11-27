package de.mannodermaus.junit5.test

//class ActivityTestUnitTests {
//
//  private lateinit var extension: ActivityTestExtension
//
//  @BeforeEach
//  fun beforeEach() {
//    extension = ActivityTestExtension()
//  }
//
////  @Test
////  fun supportsParameterForUnknownActivitySubtypeThrows() {
////    Assertions.assertThrows(UnexpectedActivityType::class.java) {
////      extension.supportsParameter(
////          parameterContextOf(Tested::class.java)
////      )
////    }
////  }
//
//  /* Private */
//
//  private fun parameterContextOf(type: Type): ParameterContext {
//    val context = Mockito.mock(ParameterContext::class.java)
//    val param = Mockito.mock(Parameter::class.java)
//
//    Mockito.`when`(param.parameterizedType).thenReturn(type)
//    if (type is Class<*>) Mockito.`when`(param.type).thenReturn(type)
//
//    Mockito.`when`(context.parameter).thenReturn(param)
//    return context
//  }
//
//  private fun extensionContextOf(config: ActivityTest): ExtensionContext {
//    val context = Mockito.mock(ExtensionContext::class.java)
//    val element = SimpleAnnotatedElement(config)
//
//    Mockito.`when`(context.element).thenReturn(Optional.of(element))
//    return context
//  }
//}
//
///* Helper Types */
//
//private class SimpleAnnotatedElement(val annotation: Annotation) : AnnotatedElement {
//
//  override fun getAnnotations() = arrayOf(annotation)
//  override fun getDeclaredAnnotations() = annotations
//  override fun <T : Annotation> getAnnotation(p0: Class<T>?): T? {
//    if (p0 == null) return null
//    if (p0 != annotation::class.java) return null
//    return annotation as T
//  }
//}
